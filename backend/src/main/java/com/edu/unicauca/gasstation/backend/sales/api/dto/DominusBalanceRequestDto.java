package com.edu.unicauca.gasstation.backend.sales.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

@Schema(description = "Accumulated cash balance that Dominus reports for the day (\"SALDO ACUMULADO\")")
public record DominusBalanceRequestDto(
        @Schema(description = "Accumulated cash according to Dominus (accumulatedCash.accumulatedBalance)",
                example = "24629469")
        @NotNull(message = "El saldo acumulado de Dominus es obligatorio")
        @Digits(integer = 12, fraction = 2, message = "El saldo acumulado admite máximo 2 decimales")
        BigDecimal accumulatedBalance) {
}
