package com.edu.unicauca.gasstation.backend.inventory.domain.models;

import com.edu.unicauca.gasstation.backend.inventory.FuelType;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
public class TankSnapshot {

    private final Long tankId;
    private final String tankCode;
    private final FuelType fuelType;
    private final BigDecimal heightCm;
    private final BigDecimal gallons;
    private final BigDecimal capacityGallons;
    private final BigDecimal percentage;
    private final LocalDate measuredOn;
    private final LocalDateTime measuredAt;
    private final AutonomyLevel alertLevel;
}