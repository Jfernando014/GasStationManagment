package com.edu.unicauca.gasstation.backend.sales.infrastructure.services;

import com.edu.unicauca.gasstation.backend.sales.domain.models.FundLedger;
import com.edu.unicauca.gasstation.backend.sales.domain.models.LedgerEntry;
import com.edu.unicauca.gasstation.backend.sales.domain.models.SavingsFundMovement;
import com.edu.unicauca.gasstation.backend.sales.domain.models.SavingsFundMovementType;
import com.edu.unicauca.gasstation.backend.sales.domain.services.SavingsFundService;
import com.edu.unicauca.gasstation.backend.sales.exception.DuplicateContributionException;
import com.edu.unicauca.gasstation.backend.sales.exception.InsufficientFundException;
import com.edu.unicauca.gasstation.backend.sales.infrastructure.persistence.SavingsFundMovementRepository;
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
        if (repository.existsByMovementTypeAndMovementDate(SavingsFundMovementType.CONTRIBUTION, date)) {
            throw new DuplicateContributionException(date);
        }
        return repository.save(new SavingsFundMovement(
                SavingsFundMovementType.CONTRIBUTION, date, amount, normalize(description)));
    }

    @Override
    @Transactional
    public SavingsFundMovement registerPayment(LocalDate date, BigDecimal amount, String description) {
        List<LedgerEntry> entries = new ArrayList<>(repository.findAll().stream()
                .map(m -> new LedgerEntry(m.getMovementDate(), m.getMovementType().signed(m.getAmount())))
                .toList());
        entries.add(new LedgerEntry(date, SavingsFundMovementType.PAYMENT.signed(amount)));

        FundLedger.firstNegativeFrom(entries, date).ifPresent(negative -> {
            throw new InsufficientFundException(negative.date(), negative.balance());
        });

        return repository.save(new SavingsFundMovement(
                SavingsFundMovementType.PAYMENT, date, amount, normalize(description)));
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getBalanceAt(LocalDate date) {
        return repository.findByMovementDateLessThanEqual(date).stream()
                .map(m -> m.getMovementType().signed(m.getAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SavingsFundMovement> listMovements() {
        return repository.findAllByOrderByMovementDateDescIdDesc();
    }

    private static String normalize(String description) {
        return (description == null || description.isBlank()) ? null : description.trim();
    }
}
