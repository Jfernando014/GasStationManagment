package com.edu.unicauca.gasstation.backend.inventory.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
public class TankReadingRequest {

    public static final String TANK_REQUIRED = "The tank is required";
    public static final String HEIGHT_REQUIRED = "The height is required";
    public static final String HEIGHT_NOT_NEGATIVE = "The height cannot be negative";
    public static final String HEIGHT_DIGITS = "The height must have at most 4 integer digits and 1 decimal";

    @Schema(description = "Tank id", example = "1")
    @NotNull(message = TANK_REQUIRED)
    private Long tankId;

    @Schema(description = "Height measured with the ruler, in centimeters", example = "55.5")
    @NotNull(message = HEIGHT_REQUIRED)
    @DecimalMin(value = "0", message = HEIGHT_NOT_NEGATIVE)
    @Digits(integer = 4, fraction = 1, message = HEIGHT_DIGITS)
    private BigDecimal heightCm;
}