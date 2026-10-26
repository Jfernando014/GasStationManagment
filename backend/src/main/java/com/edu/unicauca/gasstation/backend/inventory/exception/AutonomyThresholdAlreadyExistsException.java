package com.edu.unicauca.gasstation.backend.inventory.exception;

import java.time.LocalDate;

public class AutonomyThresholdAlreadyExistsException extends RuntimeException {

    public AutonomyThresholdAlreadyExistsException(LocalDate validFrom) {
        super("A threshold with validity date " + validFrom + " already exists");
    }
}