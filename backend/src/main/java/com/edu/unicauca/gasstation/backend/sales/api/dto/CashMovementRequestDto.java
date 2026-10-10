package com.edu.unicauca.gasstation.backend.sales.api.dto;

import com.edu.unicauca.gasstation.backend.sales.domain.models.CashMovementType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "Cash movement: initial balance, deposit or adjustment")
public record CashMovementRequestDto(
        @Schema(description = "INITIAL_BALANCE, DEPOSIT or ADJUSTMENT", example = "INITIAL_BALANCE")
        @NotNull CashMovementType type,
        @Schema(example = "2026-08-01") @NotNull LocalDate date,
        @Schema(description = "Amount in COP. Only ADJUSTMENT may be negative", example = "500000")
        @NotNull @Digits(integer = 12, fraction = 2) BigDecimal amount,
        @Size(max = 255) String description) {
}
