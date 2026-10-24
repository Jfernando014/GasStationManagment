package com.edu.unicauca.gasstation.backend.sales.domain.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.edu.unicauca.gasstation.backend.sales.domain.Journal;
import com.edu.unicauca.gasstation.backend.sales.domain.JournalRepository;
import com.edu.unicauca.gasstation.backend.sales.domain.JournalStatus;
import com.edu.unicauca.gasstation.backend.sales.domain.models.DiscrepancyToleranceConfig;
import com.edu.unicauca.gasstation.backend.sales.domain.repositories.DiscrepancyToleranceRepository;
import com.edu.unicauca.gasstation.backend.sales.exception.InvalidToleranceConfigurationException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DiscrepancyToleranceServiceTest {

    private final List<DiscrepancyToleranceConfig> store = new ArrayList<>();
    private DiscrepancyToleranceService service;
    private JournalRepository journalRepository;

    @BeforeEach
    void setUp() {
        store.clear();
        AtomicLong ids = new AtomicLong();
        journalRepository = mock(JournalRepository.class);

        DiscrepancyToleranceRepository repository = new DiscrepancyToleranceRepository() {
            @Override
            public Optional<DiscrepancyToleranceConfig> findEffectiveTolerance(LocalDate date) {
                return store.stream()
                        .filter(c -> !c.effectiveDate().isAfter(date))
                        .max(Comparator.comparing(DiscrepancyToleranceConfig::effectiveDate));
            }

            @Override
            public Optional<DiscrepancyToleranceConfig> findByEffectiveDate(LocalDate effectiveDate) {
                return store.stream()
                        .filter(c -> c.effectiveDate().equals(effectiveDate))
                        .findFirst();
            }

            @Override
            public DiscrepancyToleranceConfig save(DiscrepancyToleranceConfig config) {
                DiscrepancyToleranceConfig saved = new DiscrepancyToleranceConfig(
                        ids.incrementAndGet(),
                        config.toleranceAmount(),
                        config.effectiveDate(),
                        config.notes(),
                        Instant.now(),
                        config.createdBy()
                );
                store.removeIf(c -> c.effectiveDate().equals(config.effectiveDate()));
                store.add(saved);
                return saved;
            }

            @Override
            public List<DiscrepancyToleranceConfig> findAllHistory() {
                return store.stream()
                        .sorted(Comparator.comparing(DiscrepancyToleranceConfig::effectiveDate).reversed())
                        .toList();
            }

            @Override
            public List<DiscrepancyToleranceConfig> findScheduledAfter(LocalDate date) {
                return store.stream()
                        .filter(c -> c.effectiveDate().isAfter(date))
                        .sorted(Comparator.comparing(DiscrepancyToleranceConfig::effectiveDate))
                        .toList();
            }
        };

        service = new DiscrepancyToleranceService(repository, journalRepository);
    }

    @Test
    void resolvesEffectiveToleranceWithTemporalPolicy() {
        // Given baseline version on 01/08 with 0 COP tolerance
        service.configureTolerance(new BigDecimal("0.00"), LocalDate.of(2026, 8, 1), "Baseline", "admin");
        // And a new policy configured from 10/08 with 500 COP tolerance
        service.configureTolerance(new BigDecimal("500.00"), LocalDate.of(2026, 8, 10), "Approved policy", "admin");

        // When evaluating day 05/08 (calibration date) -> must yield baseline 0.00
        BigDecimal toleranceDay05 = service.getEffectiveTolerance(LocalDate.of(2026, 8, 5));
        assertEquals(new BigDecimal("0.00"), toleranceDay05);

        // When evaluating day 10/08 -> must yield new 500.00
        BigDecimal toleranceDay10 = service.getEffectiveTolerance(LocalDate.of(2026, 8, 10));
        assertEquals(new BigDecimal("500.00"), toleranceDay10);

        // When evaluating day 15/08 -> must retain 500.00
        BigDecimal toleranceDay15 = service.getEffectiveTolerance(LocalDate.of(2026, 8, 15));
        assertEquals(new BigDecimal("500.00"), toleranceDay15);
    }

    @Test
    void rejectsNegativeToleranceAmount() {
        assertThrows(InvalidToleranceConfigurationException.class, () ->
                service.configureTolerance(new BigDecimal("-100.00"), LocalDate.of(2026, 8, 10), "Invalid", "admin"));
    }

    @Test
    void rejectsConfigurationWhenJournalIsAlreadyClosed() {
        LocalDate closedDate = LocalDate.of(2026, 8, 5);
        Journal closedJournal = Journal.builder()
                .id(1L)
                .date(closedDate)
                .status(JournalStatus.CLOSED)
                .build();

        when(journalRepository.findByDate(closedDate)).thenReturn(Optional.of(closedJournal));

        assertThrows(InvalidToleranceConfigurationException.class, () ->
                service.configureTolerance(new BigDecimal("300.00"), closedDate, "Attempt to alter closed day", "admin"));
    }

    @Test
    void allowsConfigurationWhenJournalIsOpen() {
        LocalDate openDate = LocalDate.of(2026, 8, 5);
        Journal openJournal = Journal.builder()
                .id(1L)
                .date(openDate)
                .status(JournalStatus.OPEN)
                .build();

        when(journalRepository.findByDate(openDate)).thenReturn(Optional.of(openJournal));

        DiscrepancyToleranceConfig config = service.configureTolerance(new BigDecimal("300.00"), openDate, "Configuring open day", "admin");
        assertNotNull(config);
        assertEquals(new BigDecimal("300.00"), config.toleranceAmount());
    }

    @Test
    void fallsBackToZeroWhenNoToleranceConfigured() {
        BigDecimal tolerance = service.getEffectiveTolerance(LocalDate.of(2026, 7, 1));
        assertEquals(BigDecimal.ZERO, tolerance);
    }
}
