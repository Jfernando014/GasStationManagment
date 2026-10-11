package com.edu.unicauca.gasstation.backend.inventory.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.edu.unicauca.gasstation.backend.inventory.FuelType;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FuelPriceRequest {

    private static final String PRICE_MESSAGE = "The price must be a positive numerical value";

    @Schema(example = "MOTOR")
    @NotNull(message = "The fuel type is required")
    private FuelType fuelType;

    @Schema(example = "16330")
    @NotNull(message = PRICE_MESSAGE)
    @Positive(message = PRICE_MESSAGE)
    @Digits(integer = 10, fraction = 2, message = PRICE_MESSAGE)
    private BigDecimal pricePerGallon;

    @Schema(example = "2026-08-01")
    @NotNull(message = "The validity date is required")
    private LocalDate validFrom;

}