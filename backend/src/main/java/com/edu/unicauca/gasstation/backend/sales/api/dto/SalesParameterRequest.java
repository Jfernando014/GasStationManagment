package com.edu.unicauca.gasstation.backend.sales.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "New value of a sales parameter, effective from a date")
public record SalesParameterRequest(
        @Schema(description = "COP for daily-savings, freight-per-gallon and financial-cost-per-gallon; fraction for "
                + "credibanco-commission-rate (2,65 % = 0.0265). Only the two per gallon keys accept zero.",
                example = "0.0265")
        @NotNull(message = "El valor es obligatorio")
        @Digits(integer = 12, fraction = 6, message = "El valor admite hasta 12 enteros y 6 decimales") BigDecimal value,
        @Schema(description = "First day on which the value applies (yyyy-MM-dd)", example = "2026-08-01")
        @NotNull(message = "La fecha de vigencia es obligatoria") LocalDate validFrom) {
}
