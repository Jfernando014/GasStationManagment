package com.edu.unicauca.gasstation.backend.sales.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "Administrative expense registered for the Sales module")
public record AdministrativeExpenseRequestDto(
        @Schema(example = "2026-08-01") @NotNull LocalDate date,
        @Schema(description = "Amount in COP", example = "350000")
        @NotNull @Digits(integer = 12, fraction = 2) BigDecimal amount,
        @Size(max = 255) String description) {
}
