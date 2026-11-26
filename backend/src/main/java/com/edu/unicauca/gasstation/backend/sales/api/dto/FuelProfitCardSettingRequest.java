package com.edu.unicauca.gasstation.backend.sales.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;

@Schema(description = "Card percentage setting of a month")
public record FuelProfitCardSettingRequest(
        @Schema(description = "REAL or MANUAL", example = "MANUAL")
        @NotNull(message = "El modo es obligatorio")
        @Pattern(regexp = "REAL|MANUAL", message = "El modo debe ser REAL o MANUAL") String mode,
        @Schema(description = "From 0 to 100. Required in MANUAL mode and ignored in REAL mode.", example = "35.5")
        @DecimalMin(value = "0", message = "El porcentaje manual debe estar entre 0 y 100")
        @DecimalMax(value = "100", message = "El porcentaje manual debe estar entre 0 y 100")
        @Digits(integer = 3, fraction = 4, message = "El porcentaje manual admite máximo 4 decimales")
        BigDecimal manualPercentage) {
}
