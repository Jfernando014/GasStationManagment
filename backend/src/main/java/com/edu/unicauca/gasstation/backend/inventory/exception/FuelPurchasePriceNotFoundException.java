package com.edu.unicauca.gasstation.backend.inventory.exception;

import com.edu.unicauca.gasstation.backend.inventory.FuelType;

import java.time.LocalDate;

public class FuelPurchasePriceNotFoundException extends RuntimeException {
    public FuelPurchasePriceNotFoundException(FuelType fuelType, LocalDate date) {
        super("There is no current price for " + fuelType + " as of " + date);
    }
    public FuelPurchasePriceNotFoundException(Long id) {
        super("There is no price with that ID " + id);
    }
}
