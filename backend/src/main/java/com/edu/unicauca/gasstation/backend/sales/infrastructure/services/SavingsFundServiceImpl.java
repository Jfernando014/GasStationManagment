package com.edu.unicauca.gasstation.backend.sales.infrastructure.services;

import com.edu.unicauca.gasstation.backend.sales.domain.models.FundLedger;
import com.edu.unicauca.gasstation.backend.sales.domain.models.LedgerEntry;
import com.edu.unicauca.gasstation.backend.sales.domain.models.SavingsFundDaySummary;
import com.edu.unicauca.gasstation.backend.sales.domain.models.SavingsFundMovement;
import com.edu.unicauca.gasstation.backend.sales.domain.models.SavingsFundMovementType;
import com.edu.unicauca.gasstation.backend.sales.domain.repositories.SavingsFundMovementRepository;
import com.edu.unicauca.gasstation.backend.sales.domain.services.SavingsFundService;
import com.edu.unicauca.gasstation.backend.sales.exception.DuplicateContributionException;
import com.edu.unicauca.gasstation.backend.sales.exception.InsufficientFundException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SavingsFundServiceImpl implements SavingsFundService {

    private final SavingsFundMovementRepository repository;

    public SavingsFundServiceImpl(SavingsFundMovementRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public SavingsFundMovement registerContribution(LocalDate date, BigDecimal amount, String description) {
        if (repository.existsContributionOn(date)) {
            throw new DuplicateContributionException(date);
        }
        return repository.save(SavingsFundMovement.create(
                SavingsFundMovementType.CONTRIBUTION, date, amount, normalize(description)));
    }

    @Override
    @Transactional
    public SavingsFundMovement registerPayment(LocalDate date, BigDecimal amount, String description) {
        BigDecimal available = getBalanceAt(date);
        if (available.compareTo(amount) < 0) {
            throw new InsufficientFundException(available, amount, date);
        }

        // A payment dated in the past must not break the balance of any later date.
        List<LedgerEntry> entries = new ArrayList<>(repository.findAll().stream()
                .map(m -> new LedgerEntry(m.getMovementDate(), m.signedAmount()))
                .toList());
        entries.add(new LedgerEntry(date, SavingsFundMovementType.PAYMENT.signed(amount)));
        FundLedger.firstNegativeFrom(entries, date).ifPresent(negative -> {
            throw new InsufficientFundException(negative.date(), negative.balance());
        });

        return repository.save(SavingsFundMovement.create(
                SavingsFundMovementType.PAYMENT, date, amount, normalize(description)));
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getBalanceAt(LocalDate date) {
        return repository.findUpTo(date).stream()
                .map(SavingsFundMovement::signedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Override
    @Transactional(readOnly = true)
    public SavingsFundDaySummary summarize(LocalDate date) {
        BigDecimal previousBalance = BigDecimal.ZERO;
        BigDecimal contribution = BigDecimal.ZERO;
        BigDecimal payments = BigDecimal.ZERO;
        // findUpTo includes the date itself, so a movement that is not before it belongs to that day
        for (SavingsFundMovement movement : repository.findUpTo(date)) {
            if (movement.getMovementDate().isBefore(date)) {
                previousBalance = previousBalance.add(movement.signedAmount());
            } else if (movement.getMovementType() == SavingsFundMovementType.CONTRIBUTION) {
                contribution = contribution.add(movement.getAmount());
            } else {
                payments = payments.add(movement.getAmount());
            }
        }
        return new SavingsFundDaySummary(previousBalance, contribution, payments);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SavingsFundMovement> listMovements() {
        return repository.findAllNewestFirst();
    }

    private static String normalize(String description) {
        return (description == null || description.isBlank()) ? null : description.trim();
    }
}
