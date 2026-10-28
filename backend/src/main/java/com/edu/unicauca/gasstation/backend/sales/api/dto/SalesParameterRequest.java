package com.edu.unicauca.gasstation.backend.sales.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "New value of a sales parameter, effective from a date")
public record SalesParameterRequest(
        @Schema(description = "COP for daily-savings; fraction for credibanco-commission-rate (2,65 % = 0.0265)",
                example = "0.0265")
        @NotNull(message = "El valor es obligatorio") @Positive(message = "El valor debe ser mayor que cero")
        @Digits(integer = 12, fraction = 6, message = "El valor admite hasta 12 enteros y 6 decimales") BigDecimal value,
        @Schema(description = "First day on which the value applies (yyyy-MM-dd)", example = "2026-08-01")
        @NotNull(message = "La fecha de vigencia es obligatoria") LocalDate validFrom) {
}
