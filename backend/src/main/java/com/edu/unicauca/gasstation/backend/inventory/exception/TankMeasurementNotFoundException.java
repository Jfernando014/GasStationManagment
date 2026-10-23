package com.edu.unicauca.gasstation.backend.inventory.exception;

import java.time.LocalDate;

public class TankMeasurementNotFoundException extends RuntimeException {

    private TankMeasurementNotFoundException(String message) {
        super(message);
    }

    public static TankMeasurementNotFoundException forDate(LocalDate journalDate) {
        return new TankMeasurementNotFoundException("There is no tank measurement for the journal date " + journalDate);
    }

    public static TankMeasurementNotFoundException before(LocalDate journalDate) {
        return new TankMeasurementNotFoundException("There is no tank measurement before the journal date " + journalDate);
    }
}