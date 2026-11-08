package com.edu.unicauca.gasstation.backend.sales.domain.services;

import com.edu.unicauca.gasstation.backend.sales.SellerCloseQuery;
import com.edu.unicauca.gasstation.backend.sales.SellerCloseQuery.DaySummary;
import com.edu.unicauca.gasstation.backend.sales.SellerCloseQuery.VendorCloseSummary;
import com.edu.unicauca.gasstation.backend.sales.domain.Journal;
import com.edu.unicauca.gasstation.backend.sales.domain.JournalRepository;
import com.edu.unicauca.gasstation.backend.sales.domain.JournalStatus;
import com.edu.unicauca.gasstation.backend.sales.domain.JournalStep;
import com.edu.unicauca.gasstation.backend.sales.domain.StepStatus;
import com.edu.unicauca.gasstation.backend.sales.domain.repositories.DayStaffReadPort;
import com.edu.unicauca.gasstation.backend.sales.domain.repositories.DayStaffReadPort.AssignedVendor;
import com.edu.unicauca.gasstation.backend.sales.domain.repositories.InventoryClosingReadPort;
import com.edu.unicauca.gasstation.backend.sales.domain.repositories.InventoryClosingReadPort.ClosingInventorySnapshot;
import com.edu.unicauca.gasstation.backend.sales.domain.repositories.InventoryClosingReadPort.FuelVariationSummary;
import com.edu.unicauca.gasstation.backend.sales.domain.repositories.InventoryClosingReadPort.InventoryAlertSummary;
import com.edu.unicauca.gasstation.backend.sales.domain.services.JournalClosingSummaryService.ClosingSummaryResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("JournalClosingSummaryService - Step 6 Closing Summary & Inventory Alerts (S3-03, S3-05)")
class JournalClosingSummaryServiceTest {

    @Mock
    private JournalRepository journalRepository;

    @Mock
    private DayStaffReadPort dayStaffReadPort;

    @Mock
    private SellerCloseQuery sellerCloseQuery;

    @Mock
    private InventoryClosingReadPort inventoryClosingReadPort;

    @InjectMocks
    private JournalClosingSummaryService closingSummaryService;

    @Test
    @DisplayName("Should include fuel variations, new autonomy alerts and step discrepancies in closing summary")
    void buildsClosingSummaryWithInventoryVariationsAndNewAlerts() {
        LocalDate date = LocalDate.of(2026, 8, 5);
        Journal journal = Journal.builder()
                .id(5L)
                .date(date)
                .consecutive(5L)
                .status(JournalStatus.OPEN)
                .steps(List.of(
                        JournalStep.builder().stepNumber(1).name("Paso 1").status(StepStatus.COMPLETED).build(),
                        JournalStep.builder().stepNumber(2).name("Paso 2").status(StepStatus.COMPLETED).build(),
                        JournalStep.builder().stepNumber(3).name("Paso 3").status(StepStatus.COMPLETED).build(),
                        JournalStep.builder().stepNumber(4).name("Paso 4").status(StepStatus.COMPLETED).build(),
                        JournalStep.builder().stepNumber(5).name("Paso 5").status(StepStatus.COMPLETED).build(),
                        JournalStep.builder().stepNumber(6).name("Paso 6").status(StepStatus.PENDING).build()
                ))
                .build();

        when(journalRepository.findById(5L)).thenReturn(Optional.of(journal));
        when(dayStaffReadPort.findStaffForDate(date)).thenReturn(List.of(
                new AssignedVendor("1001", "Carlos", "TITULAR", "DIA6", 1, "DIA6"),
                new AssignedVendor("1002", "Mónica", "APOYO", "12-7", 1, "12-7"),
                new AssignedVendor("1003", "Wiltom", "TITULAR", "NOCHE", 1, "NOCHE")
        ));

        VendorCloseSummary carlos = new VendorCloseSummary(
                date, "1001", "Carlos", "DIA6", 1,
                new BigDecimal("2500000"), BigDecimal.ZERO, new BigDecimal("1800000"),
                new BigDecimal("300000"), BigDecimal.ZERO, new BigDecimal("4600000"),
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, new BigDecimal("4600531.42"), new BigDecimal("-531.42"),
                "FALTA", List.of()
        );
        VendorCloseSummary monica = new VendorCloseSummary(
                date, "1002", "Mónica", "12-7", 1,
                new BigDecimal("2373000"), BigDecimal.ZERO, new BigDecimal("1166042"),
                new BigDecimal("351000"), BigDecimal.ZERO, new BigDecimal("3890042"),
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, new BigDecimal("3890028.87"), new BigDecimal("13.13"),
                "SOBRA", List.of()
        );

        DaySummary daySummary = new DaySummary(
                date,
                new BigDecimal("7530000"),
                BigDecimal.ZERO,
                new BigDecimal("5691312"),
                new BigDecimal("876008"),
                new BigDecimal("41380"),
                new BigDecimal("14138700"),
                new BigDecimal("14139218.29"),
                new BigDecimal("-518.29"),
                new BigDecimal("520"),
                new BigDecimal("310"),
                new BigDecimal("85"),
                new BigDecimal("40"),
                new BigDecimal("955"),
                BigDecimal.ZERO,
                0,
                List.of()
        );

        when(sellerCloseQuery.findVendorClosesByDate(date)).thenReturn(List.of(carlos, monica));
        when(sellerCloseQuery.findDaySummary(date)).thenReturn(Optional.of(daySummary));

        FuelVariationSummary motorVar = new FuelVariationSummary(
                "MOTOR",
                new BigDecimal("4200"),
                new BigDecimal("3970"),
                new BigDecimal("520"),
                new BigDecimal("7650"),
                new BigDecimal("7709.9"),
                new BigDecimal("59.9"),
                new BigDecimal("75.0"),
                false,
                "OK"
        );
        InventoryAlertSummary warningAlert = new InventoryAlertSummary(
                10L, "AUTONOMY", "EXTRA", "WARNING", "ACTIVE", new BigDecimal("3.50"), date
        );
        InventoryAlertSummary criticalAlert = new InventoryAlertSummary(
                11L, "AUTONOMY", "DIESEL", "CRITICAL", "ACTIVE", new BigDecimal("1.20"), date
        );

        when(inventoryClosingReadPort.findClosingSnapshot(date)).thenReturn(
                new ClosingInventorySnapshot(true, List.of(motorVar), List.of(warningAlert, criticalAlert))
        );

        ClosingSummaryResult summary = closingSummaryService.getClosingSummary(5L);

        assertThat(summary.journalId()).isEqualTo(5L);
        assertThat(summary.completedStepsCount()).isEqualTo(5);
        assertThat(summary.vendorsWithDiscrepancyCount()).isEqualTo(2);
        assertThat(summary.faltaCount()).isEqualTo(1);
        assertThat(summary.sobraCount()).isEqualTo(1);
        assertThat(summary.newAlertsCount()).isEqualTo(2);
        // Critical alert should be sorted first
        assertThat(summary.newAlerts()).extracting(InventoryAlertSummary::fuelType)
                .containsExactly("DIESEL", "EXTRA");
        assertThat(summary.variations()).hasSize(1);
        assertThat(summary.variations().get(0).varianceGallons()).isEqualByComparingTo("59.9");
        assertThat(summary.stepsSummary()).hasSize(5);
        assertThat(summary.stepsSummary().get(1).displayStatus()).isEqualTo("WITH_DISCREPANCY");
        assertThat(summary.stepsSummary().get(4).summaryText()).contains("2 alerta(s) nueva(s)");
    }
}
