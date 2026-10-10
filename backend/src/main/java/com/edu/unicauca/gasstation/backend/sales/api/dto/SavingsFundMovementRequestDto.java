package com.edu.unicauca.gasstation.backend.sales.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "Contribution or payment of the savings fund")
public record SavingsFundMovementRequestDto(
        @Schema(description = "Movement date (yyyy-MM-dd)", example = "2026-08-01")
        @NotNull LocalDate date,
        @Schema(description = "Amount in COP", example = "250000")
        @NotNull @Positive @Digits(integer = 12, fraction = 2) BigDecimal amount,
        @Schema(description = "Optional note", example = "Daily contribution")
        @Size(max = 255) String description) {
}
