package com.edu.unicauca.gasstation.backend.inventory.api.dto;

import com.edu.unicauca.gasstation.backend.inventory.FuelType;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.AutonomyLevel;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "Stock of a fuel according to the last measurement of its tanks")
public class FuelStockSummaryResponse {

    private FuelType fuelType;
    private BigDecimal currentGallons;
    private BigDecimal capacityGallons;

    @Schema(description = "Current over capacity, one decimal. Null if the capacity is unknown")
    private BigDecimal percentage;

    @Schema(description = "False if some tank of the fuel has never been measured")
    private boolean complete;

    @Schema(description = "Level of the open alert of the fuel. Null if it has none")
    private AutonomyLevel alertLevel;
}