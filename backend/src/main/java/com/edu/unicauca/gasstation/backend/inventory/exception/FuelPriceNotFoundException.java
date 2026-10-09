package com.edu.unicauca.gasstation.backend.inventory.exception;

import com.edu.unicauca.gasstation.backend.inventory.domain.models.FuelType;

import java.time.LocalDate;

public class FuelPriceNotFoundException extends RuntimeException {
    public FuelPriceNotFoundException(FuelType fuelType, LocalDate date) {
        super("There is no current price for " + fuelType + " as of " + date);
    }
    public FuelPriceNotFoundException(Long id) {
        super("There is no price with that ID " + id);
    }
}
