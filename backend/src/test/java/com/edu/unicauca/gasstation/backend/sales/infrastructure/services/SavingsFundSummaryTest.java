package com.edu.unicauca.gasstation.backend.sales.infrastructure.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;

import com.edu.unicauca.gasstation.backend.sales.domain.models.SavingsFundDaySummary;
import com.edu.unicauca.gasstation.backend.sales.domain.models.SavingsFundMovement;
import com.edu.unicauca.gasstation.backend.sales.domain.models.SavingsFundMovementType;
import com.edu.unicauca.gasstation.backend.sales.domain.repositories.SavingsFundMovementRepository;
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
class SavingsFundSummaryTest {

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
        lenient().when(repository.existsContributionOn(any())).thenAnswer(i ->
                store.stream().anyMatch(m -> m.getMovementType() == SavingsFundMovementType.CONTRIBUTION
                        && m.getMovementDate().equals(i.getArgument(0))));
        lenient().when(repository.findUpTo(any())).thenAnswer(i -> {
            LocalDate date = i.getArgument(0);
            return store.stream().filter(m -> !m.getMovementDate().isAfter(date)).toList();
        });
        lenient().when(repository.findAll()).thenAnswer(i -> new ArrayList<>(store));
        service = new SavingsFundServiceImpl(repository);
    }

    private static BigDecimal cop(long value) {
        return BigDecimal.valueOf(value);
    }

    private static LocalDate august(int day) {
        return LocalDate.of(2026, 8, day);
    }

    private void saveEveryDay(int fromDay, int toDay) {
        for (int day = fromDay; day <= toDay; day++) {
            service.registerContribution(august(day), cop(250_000), null);
        }
    }

    @Test
    void anEmptyFundSummarizesToZero() {
        SavingsFundDaySummary summary = service.summarize(august(5));

        assertEquals(0, cop(0).compareTo(summary.previousBalance()));
        assertEquals(0, cop(0).compareTo(summary.contribution()));
        assertEquals(0, cop(0).compareTo(summary.payments()));
        assertEquals(0, cop(0).compareTo(summary.accumulatedBalance()));
    }

    @Test
    void addsTheDailyContributionToThePreviousBalance() {
        saveEveryDay(1, 16);

        SavingsFundDaySummary summary = service.summarize(august(16));

        assertEquals(0, cop(3_750_000).compareTo(summary.previousBalance()));
        assertEquals(0, cop(250_000).compareTo(summary.contribution()));
        assertEquals(0, cop(0).compareTo(summary.payments()));
        assertEquals(0, cop(4_000_000).compareTo(summary.accumulatedBalance()));
    }

    @Test
    void aPaymentOfTheSameDayLowersTheAccumulatedBalance() {
        saveEveryDay(1, 16);
        service.registerPayment(august(16), cop(1_000_000), "Rent");

        SavingsFundDaySummary summary = service.summarize(august(16));

        assertEquals(0, cop(3_750_000).compareTo(summary.previousBalance()));
        assertEquals(0, cop(250_000).compareTo(summary.contribution()));
        assertEquals(0, cop(1_000_000).compareTo(summary.payments()));
        assertEquals(0, cop(3_000_000).compareTo(summary.accumulatedBalance()));
    }

    @Test
    void aDayWithoutMovementsKeepsTheBalance() {
        saveEveryDay(1, 16);

        SavingsFundDaySummary summary = service.summarize(august(20));

        assertEquals(0, cop(4_000_000).compareTo(summary.previousBalance()));
        assertEquals(0, cop(0).compareTo(summary.contribution()));
        assertEquals(0, cop(4_000_000).compareTo(summary.accumulatedBalance()));
    }

    @Test
    void ignoresMovementsAfterTheDate() {
        saveEveryDay(1, 16);

        SavingsFundDaySummary summary = service.summarize(august(5));

        assertEquals(0, cop(1_000_000).compareTo(summary.previousBalance()));
        assertEquals(0, cop(1_250_000).compareTo(summary.accumulatedBalance()));
    }

    @Test
    void accumulatedBalanceAlwaysMatchesGetBalanceAt() {
        saveEveryDay(1, 16);
        service.registerPayment(august(10), cop(500_000), "Partial payment");

        for (int day = 1; day <= 20; day++) {
            assertEquals(0, service.getBalanceAt(august(day)).compareTo(service.summarize(august(day)).accumulatedBalance()),
                    "Different balance on August " + day);
        }
    }
}
