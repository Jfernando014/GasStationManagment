package com.edu.unicauca.gasstation.backend.sales.api.dto;

import com.edu.unicauca.gasstation.backend.sales.domain.JournalStatus;
import com.edu.unicauca.gasstation.backend.sales.domain.repositories.InventoryClosingReadPort.FuelVariationSummary;
import com.edu.unicauca.gasstation.backend.sales.domain.repositories.InventoryClosingReadPort.InventoryAlertSummary;
import com.edu.unicauca.gasstation.backend.sales.domain.services.JournalClosingSummaryService.ClosingSummaryResult;
import com.edu.unicauca.gasstation.backend.sales.domain.services.JournalClosingSummaryService.StepSummaryItem;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "Step 6 closing summary for an operational journal (C-08, S3-03, S3-05)")
public record JournalClosingSummaryResponse(
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
) {
    public static JournalClosingSummaryResponse from(ClosingSummaryResult result) {
        return new JournalClosingSummaryResponse(
                result.journalId(),
                result.journalDate(),
                result.consecutive(),
                result.status(),
                result.closedAt(),
                result.completedStepsCount(),
                result.totalStepsToComplete(),
                result.vendorsWithDiscrepancyCount(),
                result.faltaCount(),
                result.sobraCount(),
                result.newAlertsCount(),
                result.stepsSummary(),
                result.variations(),
                result.newAlerts()
        );
    }
}
