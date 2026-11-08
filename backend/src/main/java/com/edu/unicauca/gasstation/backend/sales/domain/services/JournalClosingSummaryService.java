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
import com.edu.unicauca.gasstation.backend.sales.exception.JournalNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Domain service that builds the consolidated Step 6 closing summary (C-08, S3-03, S3-05),
 * combining the status of steps 1..5, vendor discrepancies, fuel variations, and new inventory alerts.
 */
@Service
@RequiredArgsConstructor
public class JournalClosingSummaryService {

    private static final Map<Integer, String> STEP_TITLES = Map.of(
            1, "Personal del día",
            2, "Cierre por vendedor",
            3, "Medios electrónicos",
            4, "Consolidado y caja",
            5, "Medición y existencias"
    );

    private final JournalRepository journalRepository;
    private final DayStaffReadPort dayStaffReadPort;
    private final SellerCloseQuery sellerCloseQuery;
    private final InventoryClosingReadPort inventoryClosingReadPort;

    @Transactional(readOnly = true)
    public ClosingSummaryResult getClosingSummary(Long journalId) {
        Journal journal = journalRepository.findById(journalId)
                .orElseThrow(() -> new JournalNotFoundException(journalId));

        LocalDate date = journal.getDate();
        Map<Integer, JournalStep> stepsByNumber = (journal.getSteps() != null ? journal.getSteps() : List.<JournalStep>of())
                .stream()
                .filter(s -> s.getStepNumber() != null)
                .collect(Collectors.toMap(JournalStep::getStepNumber, s -> s, (a, b) -> a));

        List<AssignedVendor> assignedVendors = dayStaffReadPort.findStaffForDate(date);
        List<VendorCloseSummary> vendorCloses = sellerCloseQuery.findVendorClosesByDate(date);
        Optional<DaySummary> daySummaryOpt = sellerCloseQuery.findDaySummary(date);
        ClosingInventorySnapshot inventorySnapshot = inventoryClosingReadPort.findClosingSnapshot(date);

        int faltaCount = (int) vendorCloses.stream()
                .filter(v -> "FALTA".equalsIgnoreCase(v.resultStatus()))
                .count();
        int sobraCount = (int) vendorCloses.stream()
                .filter(v -> "SOBRA".equalsIgnoreCase(v.resultStatus()))
                .count();
        int vendorsWithDiscrepancyCount = faltaCount + sobraCount;

        List<StepSummaryItem> stepSummaries = new ArrayList<>();
        int completedStepsCount = 0;

        for (int stepNum = 1; stepNum <= 5; stepNum++) {
            JournalStep step = stepsByNumber.get(stepNum);
            boolean isCompleted = step != null && step.getStatus() == StepStatus.COMPLETED;
            if (isCompleted) {
                completedStepsCount++;
            }

            boolean hasDiscrepancy = computeStepDiscrepancy(stepNum, vendorsWithDiscrepancyCount, inventorySnapshot);
            String displayStatus = computeDisplayStatus(stepNum, step, stepsByNumber, hasDiscrepancy);
            String summaryText = buildStepSummaryText(
                    stepNum, isCompleted, assignedVendors, vendorCloses, daySummaryOpt.orElse(null),
                    vendorsWithDiscrepancyCount, inventorySnapshot
            );

            stepSummaries.add(new StepSummaryItem(
                    stepNum,
                    STEP_TITLES.getOrDefault(stepNum, "Paso " + stepNum),
                    displayStatus,
                    isCompleted,
                    hasDiscrepancy,
                    summaryText,
                    step != null ? step.getCompletedAt() : null
            ));
        }

        JournalStep step6 = stepsByNumber.get(6);
        LocalDateTime closedAt = step6 != null ? step6.getCompletedAt() : null;

        List<InventoryAlertSummary> sortedAlerts = inventorySnapshot.newAlerts().stream()
                .sorted(Comparator.comparingInt(a -> "CRITICAL".equalsIgnoreCase(a.level()) ? 0 : 1))
                .toList();

        return new ClosingSummaryResult(
                journal.getId(),
                date,
                journal.getConsecutive(),
                journal.getStatus(),
                closedAt,
                completedStepsCount,
                5,
                vendorsWithDiscrepancyCount,
                faltaCount,
                sobraCount,
                sortedAlerts.size(),
                stepSummaries,
                inventorySnapshot.variations(),
                sortedAlerts
        );
    }

    private boolean computeStepDiscrepancy(
            int stepNumber,
            int vendorsWithDiscrepancyCount,
            ClosingInventorySnapshot inventorySnapshot) {
        if (stepNumber == 2) {
            return vendorsWithDiscrepancyCount > 0;
        }
        if (stepNumber == 5) {
            return inventorySnapshot.variations().stream().anyMatch(FuelVariationSummary::outOfTolerance);
        }
        return false;
    }

    private String computeDisplayStatus(
            int stepNumber,
            JournalStep step,
            Map<Integer, JournalStep> stepsByNumber,
            boolean hasDiscrepancy) {
        if (step != null && step.getStatus() == StepStatus.COMPLETED) {
            return hasDiscrepancy ? "WITH_DISCREPANCY" : "COMPLETED";
        }
        boolean step2Done = Optional.ofNullable(stepsByNumber.get(2))
                .map(s -> s.getStatus() == StepStatus.COMPLETED)
                .orElse(false);
        boolean step3Done = Optional.ofNullable(stepsByNumber.get(3))
                .map(s -> s.getStatus() == StepStatus.COMPLETED)
                .orElse(false);

        if (stepNumber == 4 && (!step2Done || !step3Done)) {
            return "BLOCKED";
        }
        if (stepNumber == 5 && !step2Done) {
            return "BLOCKED";
        }
        if (step != null && step.getStatus() == StepStatus.IN_PROGRESS) {
            return "IN_PROGRESS";
        }
        return "PENDING";
    }

    private String buildStepSummaryText(
            int stepNumber,
            boolean isCompleted,
            List<AssignedVendor> assignedVendors,
            List<VendorCloseSummary> vendorCloses,
            DaySummary daySummary,
            int vendorsWithDiscrepancyCount,
            ClosingInventorySnapshot inventorySnapshot) {
        return switch (stepNumber) {
            case 1 -> {
                if (assignedVendors.isEmpty()) {
                    yield isCompleted ? "Personal del día confirmado" : "Sin confirmar personal del día";
                }
                yield assignedVendors.size() + " vendedores asignados";
            }
            case 2 -> {
                if (daySummary == null || vendorCloses.isEmpty()) {
                    yield "Sin cierres de vendedores registrados";
                }
                String discText = vendorsWithDiscrepancyCount == 0
                        ? "todos cuadran"
                        : vendorsWithDiscrepancyCount + " con descuadre";
                yield "Total entregado $" + daySummary.totalDelivered().toPlainString() + " · " + discText;
            }
            case 3 -> {
                if (daySummary == null) {
                    yield isCompleted ? "Medios electrónicos conciliados" : "Pendiente de conciliar medios electrónicos";
                }
                BigDecimal credibanco = daySummary.totalCredibanco() != null ? daySummary.totalCredibanco() : BigDecimal.ZERO;
                BigDecimal qr = daySummary.totalQr() != null ? daySummary.totalQr() : BigDecimal.ZERO;
                BigDecimal pts = daySummary.totalRedemptions() != null ? daySummary.totalRedemptions() : BigDecimal.ZERO;
                yield "Credibanco $" + credibanco.toPlainString()
                        + " · QR $" + qr.toPlainString()
                        + " · Puntos $" + pts.toPlainString();
            }
            case 4 -> isCompleted
                    ? "Consolidado de estación y saldo en caja verificados"
                    : "Pendiente de verificar consolidado y saldo en caja";
            case 5 -> {
                if (!inventorySnapshot.hasMeasurement() && inventorySnapshot.variations().isEmpty()) {
                    yield isCompleted ? "Medición y existencias registradas" : "Sin medición de tanques registrada";
                }
                long outCount = inventorySnapshot.variations().stream()
                        .filter(FuelVariationSummary::outOfTolerance)
                        .count();
                String varText = outCount == 0
                        ? "Variaciones dentro de tolerancia"
                        : outCount + " combustible(s) fuera de tolerancia";
                int alertsCount = inventorySnapshot.newAlerts().size();
                yield varText + " · " + alertsCount + " alerta(s) nueva(s)";
            }
            default -> "";
        };
    }

    public record ClosingSummaryResult(
            Long journalId,
            LocalDate journalDate,
            Long consecutive,
            JournalStatus status,
            LocalDateTime closedAt,
            int completedStepsCount,
            int totalStepsToComplete,
            int vendorsWithDiscrepancyCount,
            int faltaCount,
            int sobraCount,
            int newAlertsCount,
            List<StepSummaryItem> stepsSummary,
            List<FuelVariationSummary> variations,
            List<InventoryAlertSummary> newAlerts
    ) {}

    public record StepSummaryItem(
            int stepNumber,
            String title,
            String displayStatus,
            boolean completed,
            boolean hasDiscrepancy,
            String summaryText,
            LocalDateTime completedAt
    ) {}
}
