package com.edu.unicauca.gasstation.backend.sales.api.dto;

import com.edu.unicauca.gasstation.backend.sales.domain.models.DiscrepancyToleranceConfig;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Schema(description = "Discrepancy tolerance policy record")
public record DiscrepancyToleranceResponseDto(
        Long id,
        BigDecimal toleranceAmount,
        LocalDate effectiveDate,
        String notes,
        Instant createdAt,
        String createdBy
) {
    public static DiscrepancyToleranceResponseDto from(DiscrepancyToleranceConfig domain) {
        if (domain == null) return null;
        return new DiscrepancyToleranceResponseDto(
                domain.id(),
                domain.toleranceAmount(),
                domain.effectiveDate(),
                domain.notes(),
                domain.createdAt(),
                domain.createdBy()
        );
    }
}
