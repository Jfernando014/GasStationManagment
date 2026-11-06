package com.edu.unicauca.gasstation.backend.inventory.domain.models;

import com.edu.unicauca.gasstation.backend.inventory.FuelType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
public class FuelControl {

    private final FuelType fuelType;
    private final FuelVariation variation;
    private final FuelAutonomy autonomy;
    private final LocalDate estimatedDepletionDate;
}