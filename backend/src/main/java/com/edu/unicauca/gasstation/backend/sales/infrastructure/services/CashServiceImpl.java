package com.edu.unicauca.gasstation.backend.sales.infrastructure.services;

import com.edu.unicauca.gasstation.backend.sales.domain.models.CashMovement;
import com.edu.unicauca.gasstation.backend.sales.domain.models.CashMovementType;
import com.edu.unicauca.gasstation.backend.sales.domain.services.CashService;
import com.edu.unicauca.gasstation.backend.sales.exception.InvalidCashMovementException;
import com.edu.unicauca.gasstation.backend.sales.infrastructure.persistence.CashMovementRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CashServiceImpl implements CashService {

    private final CashMovementRepository repository;

    public CashServiceImpl(CashMovementRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public CashMovement register(CashMovementType type, LocalDate date, BigDecimal amount, String description) {
        if (amount.signum() == 0) {
            throw new InvalidCashMovementException("Amount must not be zero");
        }
        if (amount.signum() < 0 && !type.allowsNegativeAmount()) {
            throw new InvalidCashMovementException(
                    "Amount must be positive for " + type + "; only ADJUSTMENT can be negative");
        }
        return repository.save(new CashMovement(type, date, amount, normalize(description)));
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
    public List<CashMovement> listMovements() {
        return repository.findAllByOrderByMovementDateDescIdDesc();
    }

    private static String normalize(String description) {
        return (description == null || description.isBlank()) ? null : description.trim();
    }
}
