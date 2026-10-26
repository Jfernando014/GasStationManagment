package com.edu.unicauca.gasstation.backend.inventory.domain.models;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
public class TankStock {

    private final Long tankId;
    private final String tankCode;
    private final BigDecimal heightCm;
    private final BigDecimal gallons;
}