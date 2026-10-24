package com.edu.unicauca.gasstation.backend.sales.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "Payload to configure a new vendor discrepancy tolerance version")
public record DiscrepancyToleranceRequestDto(
        @Schema(description = "Tolerance amount in integer COP (non-negative)", example = "500.00")
        @NotNull(message = "Tolerance amount is mandatory")
        @DecimalMin(value = "0.0", message = "Tolerance amount must be non-negative")
        BigDecimal toleranceAmount,

        @Schema(description = "Date from which this tolerance becomes effective", example = "2026-08-10")
        @NotNull(message = "Effective date is mandatory")
        LocalDate effectiveDate,

        @Schema(description = "Reason or administrative notes for this version", example = "Approved management margin")
        String notes
) {}
