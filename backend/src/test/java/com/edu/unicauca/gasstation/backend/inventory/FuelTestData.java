package com.edu.unicauca.gasstation.backend.inventory;

import com.edu.unicauca.gasstation.backend.inventory.domain.models.Tank;

import java.math.BigDecimal;

public final class FuelTestData {

    private FuelTestData() {
    }

    public static Tank tank(Long id, String code, FuelType fuelType, String toleranceCm) {
        Tank tank = new Tank();
        tank.setId(id);
        tank.setCode(code);
        tank.setFuelType(fuelType);
        tank.setToleranceCm(new BigDecimal(toleranceCm));
        return tank;
    }
}