package com.edu.unicauca.gasstation.backend.sales.infrastructure.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;

import com.edu.unicauca.gasstation.backend.sales.domain.models.CashMovement;
import com.edu.unicauca.gasstation.backend.sales.domain.models.CashMovementType;
import com.edu.unicauca.gasstation.backend.sales.exception.InvalidCashMovementException;
import com.edu.unicauca.gasstation.backend.sales.infrastructure.persistence.CashMovementRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CashServiceTest {

    @Mock
    private CashMovementRepository repository;

    private final List<CashMovement> store = new ArrayList<>();
    private CashServiceImpl service;

    @BeforeEach
    void setUp() {
        lenient().when(repository.save(any(CashMovement.class))).thenAnswer(i -> {
            CashMovement m = i.getArgument(0);
            store.add(m);
            return m;
        });
        lenient().when(repository.findByMovementDateLessThanEqual(any())).thenAnswer(i -> {
            LocalDate date = i.getArgument(0);
            return store.stream().filter(m -> !m.getMovementDate().isAfter(date)).toList();
        });
        service = new CashServiceImpl(repository);
    }

    private static BigDecimal cop(long value) {
        return BigDecimal.valueOf(value);
    }

    @Test
    void balanceCombinesInitialBalanceDepositsAndAdjustments() {
        service.register(CashMovementType.INITIAL_BALANCE, LocalDate.of(2026, 8, 1), cop(500_000), null);
        service.register(CashMovementType.DEPOSIT, LocalDate.of(2026, 8, 2), cop(200_000), "Bank deposit");
        service.register(CashMovementType.ADJUSTMENT, LocalDate.of(2026, 8, 3), cop(-10_000), "Count difference");

        assertEquals(0, cop(500_000).compareTo(service.getBalanceAt(LocalDate.of(2026, 8, 1))));
        assertEquals(0, cop(300_000).compareTo(service.getBalanceAt(LocalDate.of(2026, 8, 2))));
        assertEquals(0, cop(290_000).compareTo(service.getBalanceAt(LocalDate.of(2026, 8, 3))));
    }

    @Test
    void positiveAdjustmentIncreasesTheBalance() {
        service.register(CashMovementType.INITIAL_BALANCE, LocalDate.of(2026, 8, 1), cop(100_000), null);
        service.register(CashMovementType.ADJUSTMENT, LocalDate.of(2026, 8, 1), cop(5_000), null);

        assertEquals(0, cop(105_000).compareTo(service.getBalanceAt(LocalDate.of(2026, 8, 1))));
    }

    @Test
    void rejectsNegativeAmountForNonAdjustments() {
        assertThrows(InvalidCashMovementException.class, () ->
                service.register(CashMovementType.DEPOSIT, LocalDate.of(2026, 8, 1), cop(-1), null));
        assertThrows(InvalidCashMovementException.class, () ->
                service.register(CashMovementType.INITIAL_BALANCE, LocalDate.of(2026, 8, 1), cop(-1), null));
    }

    @Test
    void rejectsZeroAmount() {
        assertThrows(InvalidCashMovementException.class, () ->
                service.register(CashMovementType.ADJUSTMENT, LocalDate.of(2026, 8, 1), BigDecimal.ZERO, null));
    }
}
