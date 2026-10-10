package com.edu.unicauca.gasstation.backend.sales.infrastructure.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;

import com.edu.unicauca.gasstation.backend.sales.domain.models.SavingsFundMovement;
import com.edu.unicauca.gasstation.backend.sales.exception.DuplicateContributionException;
import com.edu.unicauca.gasstation.backend.sales.exception.InsufficientFundException;
import com.edu.unicauca.gasstation.backend.sales.infrastructure.persistence.SavingsFundMovementRepository;
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
class SavingsFundServiceTest {

    @Mock
    private SavingsFundMovementRepository repository;

    private final List<SavingsFundMovement> store = new ArrayList<>();
    private SavingsFundServiceImpl service;

    @BeforeEach
    void setUp() {
        lenient().when(repository.save(any(SavingsFundMovement.class))).thenAnswer(i -> {
            SavingsFundMovement m = i.getArgument(0);
            store.add(m);
            return m;
        });
        lenient().when(repository.findAll()).thenAnswer(i -> new ArrayList<>(store));
        lenient().when(repository.existsByMovementTypeAndMovementDate(any(), any())).thenAnswer(i ->
                store.stream().anyMatch(m -> m.getMovementType() == i.getArgument(0)
                        && m.getMovementDate().equals(i.getArgument(1))));
        lenient().when(repository.findByMovementDateLessThanEqual(any())).thenAnswer(i -> {
            LocalDate date = i.getArgument(0);
            return store.stream().filter(m -> !m.getMovementDate().isAfter(date)).toList();
        });
        service = new SavingsFundServiceImpl(repository);
    }

    private static BigDecimal cop(long value) {
        return BigDecimal.valueOf(value);
    }

    private void registerJulyContributions() {
        for (int day = 21; day <= 31; day++) {
            service.registerContribution(LocalDate.of(2026, 7, day), cop(250_000), "Daily contribution");
        }
    }

    @Test
    void balanceAtJuly31PlusAugust1ContributionIs3000000() {
        registerJulyContributions();
        assertEquals(0, cop(2_750_000).compareTo(service.getBalanceAt(LocalDate.of(2026, 7, 31))));

        service.registerContribution(LocalDate.of(2026, 8, 1), cop(250_000), "Daily contribution");

        assertEquals(0, cop(3_000_000).compareTo(service.getBalanceAt(LocalDate.of(2026, 8, 1))));
    }

    @Test
    void balanceAtAPastDateIgnoresLaterMovements() {
        registerJulyContributions();
        service.registerContribution(LocalDate.of(2026, 8, 1), cop(250_000), null);

        assertEquals(0, cop(2_750_000).compareTo(service.getBalanceAt(LocalDate.of(2026, 7, 31))));
    }

    @Test
    void rejectsSecondContributionOnTheSameDate() {
        service.registerContribution(LocalDate.of(2026, 8, 1), cop(250_000), null);

        assertThrows(DuplicateContributionException.class,
                () -> service.registerContribution(LocalDate.of(2026, 8, 1), cop(100_000), null));
    }

    @Test
    void paymentReducesTheBalance() {
        registerJulyContributions();

        service.registerPayment(LocalDate.of(2026, 8, 1), cop(100_000), "Supplier");

        assertEquals(0, cop(2_650_000).compareTo(service.getBalanceAt(LocalDate.of(2026, 8, 1))));
    }

    @Test
    void rejectsPaymentGreaterThanBalance() {
        registerJulyContributions();

        assertThrows(InsufficientFundException.class,
                () -> service.registerPayment(LocalDate.of(2026, 7, 31), cop(5_000_000), null));
    }

    @Test
    void rejectsBackdatedPaymentThatWouldBreakALaterBalance() {
        service.registerContribution(LocalDate.of(2026, 8, 1), cop(100), null);
        service.registerPayment(LocalDate.of(2026, 8, 5), cop(100), null);

        assertThrows(InsufficientFundException.class,
                () -> service.registerPayment(LocalDate.of(2026, 8, 3), cop(50), null));
    }

    @Test
    void emptyFundHasZeroBalance() {
        assertEquals(0, BigDecimal.ZERO.compareTo(service.getBalanceAt(LocalDate.of(2026, 7, 31))));
    }
}
