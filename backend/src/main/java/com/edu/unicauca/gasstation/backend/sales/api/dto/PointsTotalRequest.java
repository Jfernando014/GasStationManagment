package com.edu.unicauca.gasstation.backend.sales.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

@Schema(description = "Day total of Puntos Colombia, typed until the Excel reading is defined")
public record PointsTotalRequest(
        @Schema(example = "41380")
        @NotNull(message = "El total de Puntos Colombia es obligatorio")
        @DecimalMin(value = "0", message = "El total de Puntos Colombia no puede ser negativo")
        @Digits(integer = 12, fraction = 2, message = "El total de Puntos Colombia admite máximo 2 decimales") BigDecimal total) {
}
