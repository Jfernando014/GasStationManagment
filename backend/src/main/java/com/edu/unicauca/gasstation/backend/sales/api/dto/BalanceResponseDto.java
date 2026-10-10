package com.edu.unicauca.gasstation.backend.sales.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "Balance at a given date (includes that date)")
public record BalanceResponseDto(
        @Schema(example = "2026-07-31") LocalDate date,
        @Schema(description = "Balance in COP", example = "2750000") BigDecimal balance) {
}
