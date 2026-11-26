package com.edu.unicauca.gasstation.backend.inventory.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/** The whole numbers are received as decimals so that a value such as 2.5 is rejected instead of truncated. */
@Getter
@Setter
@NoArgsConstructor
@Schema(description = "Rules of the monthly variation alerts that apply from a date on")
public class VariationAlertSettingRequest {

    private static final String PERCENTAGE_RANGE =
            "El porcentaje límite acumulado debe ser mayor que 0 y menor o igual a 100";
    private static final String PERCENTAGE_DIGITS = "El porcentaje límite acumulado admite máximo 4 decimales";
    private static final String DAYS_RANGE = "Los días seguidos deben ser un entero de 1 a 10";
    private static final String LIMIT_REQUIRED = "El límite de veces es obligatorio";
    private static final String LIMIT_RANGE = "El límite de veces debe ser un entero de 1 o más";

    @NotNull(message = "La fecha de vigencia es obligatoria")
    @Schema(description = "First date the rules apply to", example = "2026-08-01")
    private LocalDate validFrom;

    @NotNull(message = "El porcentaje límite acumulado es obligatorio")
    @DecimalMin(value = "0", inclusive = false, message = PERCENTAGE_RANGE)
    @DecimalMax(value = "100", message = PERCENTAGE_RANGE)
    @Digits(integer = 3, fraction = 4, message = PERCENTAGE_DIGITS)
    @Schema(example = "0.5")
    private BigDecimal cumulativeLimitPercentage;

    @NotNull(message = "Los días seguidos son obligatorios")
    @DecimalMin(value = "1", message = DAYS_RANGE)
    @DecimalMax(value = "10", message = DAYS_RANGE)
    @Digits(integer = 2, fraction = 0, message = DAYS_RANGE)
    @Schema(example = "3")
    private BigDecimal escalationDays;

    @NotNull(message = LIMIT_REQUIRED)
    @DecimalMin(value = "1", message = LIMIT_RANGE)
    @Digits(integer = 6, fraction = 0, message = LIMIT_RANGE)
    @Schema(example = "12")
    private BigDecimal shortageLimit1To49;

    @NotNull(message = LIMIT_REQUIRED)
    @DecimalMin(value = "1", message = LIMIT_RANGE)
    @Digits(integer = 6, fraction = 0, message = LIMIT_RANGE)
    @Schema(example = "8")
    private BigDecimal shortageLimit50To74;

    @NotNull(message = LIMIT_REQUIRED)
    @DecimalMin(value = "1", message = LIMIT_RANGE)
    @Digits(integer = 6, fraction = 0, message = LIMIT_RANGE)
    @Schema(example = "6")
    private BigDecimal shortageLimit75To124;

    @NotNull(message = LIMIT_REQUIRED)
    @DecimalMin(value = "1", message = LIMIT_RANGE)
    @Digits(integer = 6, fraction = 0, message = LIMIT_RANGE)
    @Schema(example = "4")
    private BigDecimal shortageLimit125To249;

    @NotNull(message = LIMIT_REQUIRED)
    @DecimalMin(value = "1", message = LIMIT_RANGE)
    @Digits(integer = 6, fraction = 0, message = LIMIT_RANGE)
    @Schema(example = "2")
    private BigDecimal shortageLimit250To499;

    @NotNull(message = LIMIT_REQUIRED)
    @DecimalMin(value = "1", message = LIMIT_RANGE)
    @Digits(integer = 6, fraction = 0, message = LIMIT_RANGE)
    @Schema(example = "1")
    private BigDecimal shortageLimit500OrMore;
}
