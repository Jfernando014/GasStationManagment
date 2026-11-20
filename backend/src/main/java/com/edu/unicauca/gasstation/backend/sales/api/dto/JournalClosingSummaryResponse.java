package com.edu.unicauca.gasstation.backend.sales.api.dto;

import com.edu.unicauca.gasstation.backend.sales.domain.JournalStatus;
import com.edu.unicauca.gasstation.backend.sales.domain.repositories.InventoryClosingReadPort.FuelVariationSummary;
import com.edu.unicauca.gasstation.backend.sales.domain.repositories.InventoryClosingReadPort.InventoryAlertSummary;
import com.edu.unicauca.gasstation.backend.sales.domain.services.JournalClosingSummaryService.ClosingSummaryResult;
import com.edu.unicauca.gasstation.backend.sales.domain.services.JournalClosingSummaryService.StepSummaryItem;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Schema(description = "Resumen de cierre de la jornada (paso 6 y pantalla de jornada cerrada), con los nombres que usa el frontend")
public record JournalClosingSummaryResponse(
        Long journalId,
        LocalDate journalDate,
        Long consecutive,
        JournalStatus status,
        LocalDateTime closedAt,
        int completedStepsCount,
        int vendorsWithShortageCount,
        int vendorsWithSurplusCount,
        int newAlertsCount,
        List<StepItem> steps,
        List<FuelVariationItem> fuelVariations,
        List<AutonomyAlertItem> newAutonomyAlerts
) {

    private static final Map<String, String> FUEL_NAMES = Map.of(
            "MOTOR", "Motor",
            "DIESEL", "Diésel",
            "EXTRA", "Extra",
            "MAX_PRO", "Max Pro"
    );

    private static final Map<String, String> VARIATION_STATUS_TEXT = Map.of(
            "NO_PREVIOUS_MEASUREMENT", "Sin medición previa",
            "INCOMPLETE_MEASUREMENT", "Medición incompleta",
            "NO_SALES_DATA", "Sin ventas registradas"
    );

    @Schema(description = "Estado de un paso. status solo toma COMPLETED, IN_PROGRESS o PENDING; un paso completo "
            + "con descuadres viaja como COMPLETED con hasDiscrepancy en true y un paso bloqueado viaja como PENDING")
    public record StepItem(
            int stepNumber,
            String stepName,
            String status,
            boolean hasDiscrepancy,
            String summaryDetail
    ) {}

    @Schema(description = "Variación de un combustible. status es NORMAL o el texto que se muestra tal cual")
    public record FuelVariationItem(
            String fuelName,
            BigDecimal initialGallons,
            BigDecimal receivedGallons,
            BigDecimal soldGallons,
            BigDecimal theoreticalGallons,
            BigDecimal measuredGallons,
            BigDecimal variationGallons,
            BigDecimal toleranceGallons,
            String status
    ) {}

    @Schema(description = "Alerta de autonomía abierta en la fecha. La alerta es por combustible: no tiene tanque "
            + "ni fecha de agotamiento guardados, por eso tankId y reachesUntilDate vienen nulos")
    public record AutonomyAlertItem(
            String tankId,
            String fuelName,
            BigDecimal autonomyDays,
            LocalDate reachesUntilDate,
            String severity
    ) {}

    public static JournalClosingSummaryResponse from(ClosingSummaryResult result) {
        return new JournalClosingSummaryResponse(
                result.journalId(),
                result.journalDate(),
                result.consecutive(),
                result.status(),
                result.closedAt(),
                result.completedStepsCount(),
                result.faltaCount(),
                result.sobraCount(),
                result.newAlertsCount(),
                result.stepsSummary().stream().map(JournalClosingSummaryResponse::toStep).toList(),
                result.variations().stream().map(JournalClosingSummaryResponse::toVariation).toList(),
                result.newAlerts().stream().map(JournalClosingSummaryResponse::toAlert).toList()
        );
    }

    private static StepItem toStep(StepSummaryItem item) {
        String status = switch (item.displayStatus()) {
            case "COMPLETED", "WITH_DISCREPANCY" -> "COMPLETED";
            case "IN_PROGRESS" -> "IN_PROGRESS";
            default -> "PENDING";
        };
        return new StepItem(
                item.stepNumber(),
                item.title(),
                status,
                item.completed() && item.hasDiscrepancy(),
                item.summaryText()
        );
    }

    private static FuelVariationItem toVariation(FuelVariationSummary variation) {
        return new FuelVariationItem(
                fuelName(variation.fuelType()),
                variation.initialGallons(),
                variation.entriesGallons(),
                variation.soldGallons(),
                variation.theoreticalGallons(),
                variation.measuredGallons(),
                variation.varianceGallons(),
                variation.toleranceGallons(),
                variationStatus(variation)
        );
    }

    private static String variationStatus(FuelVariationSummary variation) {
        if (variation.outOfTolerance()) {
            return "Fuera de tolerancia";
        }
        if (variation.status() == null || "OK".equals(variation.status())) {
            return "NORMAL";
        }
        return VARIATION_STATUS_TEXT.getOrDefault(variation.status(), variation.status());
    }

    private static AutonomyAlertItem toAlert(InventoryAlertSummary alert) {
        return new AutonomyAlertItem(null, fuelName(alert.fuelType()), alert.autonomyDays(), null, alert.level());
    }

    static String fuelName(String fuelType) {
        return fuelType == null ? null : FUEL_NAMES.getOrDefault(fuelType, fuelType);
    }
}
