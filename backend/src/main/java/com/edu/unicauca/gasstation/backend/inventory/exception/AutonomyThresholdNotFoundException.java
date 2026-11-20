package com.edu.unicauca.gasstation.backend.inventory.exception;

public class AutonomyThresholdNotFoundException extends RuntimeException {

    public AutonomyThresholdNotFoundException(Long id) {
        super("Autonomy threshold with id " + id + " not found");
    }
}
