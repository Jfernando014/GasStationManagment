package com.edu.unicauca.gasstation.backend.inventory.api.dto;

import com.edu.unicauca.gasstation.backend.inventory.domain.models.FuelType;
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

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FuelPriceRequest {

    private static final String PRICE_MESSAGE = "The price must be greater than 0.";

    @Schema(example = "MOTOR")
    @NotNull(message = "El tipo de combustible es obligatorio")
    private FuelType fuelType;

    @Schema(example = "16330")
    @NotNull(message = PRICE_MESSAGE)
    @Positive(message = PRICE_MESSAGE)
    @Digits(integer = 10, fraction = 2, message = PRICE_MESSAGE)
    private BigDecimal pricePerGallon;

    @Schema(example = "2026-08-01")
    @NotNull(message = "La fecha de vigencia es obligatoria")
    private LocalDate validFrom;

}