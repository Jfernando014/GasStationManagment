package com.edu.unicauca.gasstation.backend.inventory.domain.models;

import com.edu.unicauca.gasstation.backend.inventory.FuelType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Builder
public class FuelAutonomy {

    private final FuelType fuelType;
    private final AutonomyStatus status;
    private final BigDecimal stockGallons;
    private final BigDecimal expectedDailySalesGallons;
    private final BigDecimal autonomyDays;
    private final AutonomyLevel level;
    private final BigDecimal warningDays;
    private final BigDecimal criticalDays;
    private final List<TankStock> tanks;
}