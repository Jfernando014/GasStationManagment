package com.edu.unicauca.gasstation.backend.inventory;

import com.edu.unicauca.gasstation.backend.inventory.domain.models.FuelPrice;
import com.edu.unicauca.gasstation.backend.inventory.FuelType;

import java.math.BigDecimal;
import java.time.LocalDate;

public final class FuelPriceTestData {

    private FuelPriceTestData() {
    }

    public static FuelPrice price(Long id, FuelType fuelType, String pricePerGallon, LocalDate validFrom) {
        FuelPrice price = new FuelPrice();
        price.setId(id);
        price.setFuelType(fuelType);
        price.setPricePerGallon(new BigDecimal(pricePerGallon));
        price.setValidFrom(validFrom);
        return price;
    }
}