package com.edu.unicauca.gasstation.backend.inventory;

import com.edu.unicauca.gasstation.backend.inventory.FuelType;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.Tank;

import java.math.BigDecimal;

public final class TankTestData {

    private TankTestData() {
    }

    public static Tank tank(Long id, String code, String name) {
        Tank tank = new Tank();
        tank.setId(id);
        tank.setCode(code);
        tank.setName(name);
        tank.setFuelType(FuelType.MOTOR);
        tank.setMaxCapacityGallons(new BigDecimal("3090"));
        tank.setMaxHeightCm(new BigDecimal("238"));
        tank.setToleranceCm(new BigDecimal("1"));
        return tank;
    }
}