package com.edu.unicauca.gasstation.backend.sales.application;

import com.edu.unicauca.gasstation.backend.sales.SellerCloseQuery.DaySummary;
import com.edu.unicauca.gasstation.backend.sales.SellerCloseQuery.VendorCloseSummary;
import com.edu.unicauca.gasstation.backend.sales.domain.Journal;
import com.edu.unicauca.gasstation.backend.sales.domain.JournalRepository;
import com.edu.unicauca.gasstation.backend.sales.domain.JournalStatus;
import com.edu.unicauca.gasstation.backend.sales.domain.JournalStep;
import com.edu.unicauca.gasstation.backend.sales.domain.StepStatus;
import com.edu.unicauca.gasstation.backend.sales.domain.models.DiscrepancyStatus;
import com.edu.unicauca.gasstation.backend.sales.domain.models.VendorShiftClose;
import com.edu.unicauca.gasstation.backend.sales.domain.repositories.DayStaffReadPort;
import com.edu.unicauca.gasstation.backend.sales.domain.repositories.FuelPriceReadPort;
import com.edu.unicauca.gasstation.backend.sales.domain.repositories.VendorShiftCloseRepository;
import com.edu.unicauca.gasstation.backend.sales.domain.services.DiscrepancyToleranceService;
import com.edu.unicauca.gasstation.backend.sales.domain.services.VendorCloseCalculationEngine;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SellerCloseQueryServiceTest {

    @Mock
    private VendorShiftCloseRepository repository;

    @Mock
    private DiscrepancyToleranceService toleranceService;

    @Mock
    private DayStaffReadPort dayStaffReadPort;

    @Mock
    private FuelPriceReadPort fuelPriceReadPort;

    private VendorCloseCalculationEngine calculationEngine;
    private SellerCloseQueryServiceImpl service;

    @BeforeEach
    void setUp() {
        calculationEngine = new VendorCloseCalculationEngine();
        service = new SellerCloseQueryServiceImpl(
                repository,
                calculationEngine,
                toleranceService,
                dayStaffReadPort,
                fuelPriceReadPort
        );
    }

    @Test
    @DisplayName("findDaySummary returns empty Optional when no closes exist for the date")
    void findDaySummary_returnsEmpty_whenNoCloses() {
        LocalDate date = LocalDate.of(2026, 8, 5);
        when(repository.findByJournalDate(date)).thenReturn(List.of());

        Optional<DaySummary> summary = service.findDaySummary(date);

        assertThat(summary).isEmpty();
    }

    @Test
    @DisplayName("findDaySummary aggregates and returns DaySummary when closes exist")
    void findDaySummary_aggregatesAndReturns_whenClosesExist() {
        LocalDate date = LocalDate.of(2026, 8, 5);
        VendorShiftClose close = new VendorShiftClose(
                UUID.randomUUID(), date, "1001", "Mónica D.", "12-7", 1,
                new BigDecimal("2373000.00"), BigDecimal.ZERO, new BigDecimal("1166042.00"),
                new BigDecimal("351000.00"), BigDecimal.ZERO, new BigDecimal("3890042.00"),
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, new BigDecimal("3890028.87"), new BigDecimal("13.13"),
                BigDecimal.ZERO, DiscrepancyStatus.SOBRA, List.of()
        );
        when(repository.findByJournalDate(date)).thenReturn(List.of(close));

        Optional<DaySummary> summary = service.findDaySummary(date);

        assertThat(summary).isPresent();
        assertThat(summary.get().totalCash()).isEqualByComparingTo("2373000.00");
        assertThat(summary.get().totalDelivered()).isEqualByComparingTo("3890042.00");
    }

    @Test
    @DisplayName("findVendorClosesByDate maps domain closes to VendorCloseSummary DTOs")
    void findVendorClosesByDate_mapsCorrectly() {
        LocalDate date = LocalDate.of(2026, 8, 5);
        VendorShiftClose close = new VendorShiftClose(
                UUID.randomUUID(), date, "1001", "Mónica D.", "12-7", 1,
                new BigDecimal("2373000.00"), BigDecimal.ZERO, new BigDecimal("1166042.00"),
                new BigDecimal("351000.00"), BigDecimal.ZERO, new BigDecimal("3890042.00"),
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, new BigDecimal("3890028.87"), new BigDecimal("13.13"),
                BigDecimal.ZERO, DiscrepancyStatus.SOBRA, List.of()
        );
        when(repository.findByJournalDate(date)).thenReturn(List.of(close));

        List<VendorCloseSummary> list = service.findVendorClosesByDate(date);

        assertThat(list).hasSize(1);
        assertThat(list.get(0).vendorName()).isEqualTo("Mónica D.");
        assertThat(list.get(0).resultStatus()).isEqualTo("SOBRA");
    }

    @Test
    @DisplayName("registerVendorCloses applies effective tolerance and saves closes")
    void registerVendorCloses_evaluatesAndSaves() {
        LocalDate date = LocalDate.of(2026, 8, 5);
        when(toleranceService.getEffectiveTolerance(date)).thenReturn(new BigDecimal("50.00"));

        VendorShiftClose input = new VendorShiftClose(
                null, date, "1001", "Mónica D.", "12-7", 1,
                new BigDecimal("2373000.00"), BigDecimal.ZERO, new BigDecimal("1166042.00"),
                new BigDecimal("351000.00"), BigDecimal.ZERO, null,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, new BigDecimal("3890028.87"), null,
                null, null, List.of()
        );

        when(repository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        List<VendorShiftClose> saved = service.registerVendorCloses(date, List.of(input));

        assertThat(saved).hasSize(1);
        // With tolerance $50.00, discrepancy +$13.13 is CUADRA
        assertThat(saved.get(0).resultStatus()).isEqualTo(DiscrepancyStatus.CUADRA);
        assertThat(saved.get(0).appliedTolerance()).isEqualByComparingTo("50.00");
        verify(repository).saveAll(anyList());
    }

    @Test
    @DisplayName("resetVendorCloses deletes closes and reverts step 2 to PENDING for open journal")
    void resetVendorCloses_deletesClosesAndReopensStep2() {
        LocalDate date = LocalDate.of(2026, 11, 19);
        JournalRepository journalRepo = org.mockito.Mockito.mock(JournalRepository.class);
        JournalStep step2 = JournalStep.builder().stepNumber(2).status(StepStatus.COMPLETED).build();
        Journal openJournal = Journal.builder().id(34L).date(date).status(JournalStatus.OPEN).steps(List.of(step2)).build();
        when(journalRepo.findByDate(date)).thenReturn(Optional.of(openJournal));

        SellerCloseQueryServiceImpl serviceWithJournal = new SellerCloseQueryServiceImpl(
                repository, calculationEngine, toleranceService, dayStaffReadPort, fuelPriceReadPort, journalRepo);

        serviceWithJournal.resetVendorCloses(date);

        assertThat(step2.getStatus()).isEqualTo(StepStatus.PENDING);
        assertThat(step2.getCompletedAt()).isNull();
        verify(journalRepo).save(openJournal);
        verify(repository).deleteByJournalDate(date);
    }

    @Test
    @DisplayName("resetVendorCloses throws IllegalStateException when journal is already closed")
    void resetVendorCloses_rejectsClosedJournal() {
        LocalDate date = LocalDate.of(2026, 8, 5);
        JournalRepository journalRepo = org.mockito.Mockito.mock(JournalRepository.class);
        Journal closedJournal = Journal.builder().id(10L).date(date).status(JournalStatus.CLOSED).steps(List.of()).build();
        when(journalRepo.findByDate(date)).thenReturn(Optional.of(closedJournal));

        SellerCloseQueryServiceImpl serviceWithJournal = new SellerCloseQueryServiceImpl(
                repository, calculationEngine, toleranceService, dayStaffReadPort, fuelPriceReadPort, journalRepo);

        assertThatThrownBy(() -> serviceWithJournal.resetVendorCloses(date))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("cerrada");
        verify(repository, never()).deleteByJournalDate(date);
    }

    @Test
    @DisplayName("getEffectiveFuelPrices delegates to fuelPriceReadPort for requested date")
    void getEffectiveFuelPrices_delegatesToPort() {
        LocalDate date = LocalDate.of(2026, 11, 19);
        Map<String, BigDecimal> expectedPrices = Map.of(
                "MOTOR", new BigDecimal("15826.00"),
                "DIESEL", new BigDecimal("11484.00"),
                "EXTRA", new BigDecimal("18500.00"),
                "MAX_PRO", new BigDecimal("17100.00")
        );
        when(fuelPriceReadPort.findEffectivePrices(date)).thenReturn(expectedPrices);

        Map<String, BigDecimal> result = service.getEffectiveFuelPrices(date);

        assertThat(result).containsExactlyInAnyOrderEntriesOf(expectedPrices);
        verify(fuelPriceReadPort).findEffectivePrices(date);
    }
}
