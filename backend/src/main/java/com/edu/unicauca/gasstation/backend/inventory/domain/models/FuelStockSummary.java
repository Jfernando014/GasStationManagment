package com.edu.unicauca.gasstation.backend.inventory.domain.models;

import com.edu.unicauca.gasstation.backend.inventory.FuelType;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class FuelStockSummary {

    private final FuelType fuelType;
    private final BigDecimal currentGallons;
    private final BigDecimal capacityGallons;
    private final BigDecimal percentage;
    private final boolean complete;
    private final AutonomyLevel alertLevel;
}