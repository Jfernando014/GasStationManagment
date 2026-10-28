package com.edu.unicauca.gasstation.backend.inventory.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "Autonomy thresholds that apply from a date on")
public class AutonomyThresholdRequest {

    public static final String VALID_FROM_REQUIRED = "The validity date is required";
    public static final String WARNING_REQUIRED = "The warning threshold is required";
    public static final String CRITICAL_REQUIRED = "The critical threshold is required";
    public static final String DAYS_POSITIVE = "The threshold must be greater than zero";
    public static final String DAYS_DIGITS = "The threshold allows up to 3 integer digits and 1 decimal";

    @NotNull(message = VALID_FROM_REQUIRED)
    @Schema(description = "First journal date the thresholds apply to", example = "2026-08-01")
    private LocalDate validFrom;

    @NotNull(message = WARNING_REQUIRED)
    @Positive(message = DAYS_POSITIVE)
    @Digits(integer = 3, fraction = 1, message = DAYS_DIGITS)
    @Schema(description = "Days of autonomy that trigger a warning", example = "5")
    private BigDecimal warningDays;

    @NotNull(message = CRITICAL_REQUIRED)
    @Positive(message = DAYS_POSITIVE)
    @Digits(integer = 3, fraction = 1, message = DAYS_DIGITS)
    @Schema(description = "Days of autonomy that trigger a critical alert. Must be lower than the warning", example = "2")
    private BigDecimal criticalDays;
}