package com.edu.unicauca.gasstation.backend.inventory.exception;

public class InspectionNotFoundException extends RuntimeException {

    public InspectionNotFoundException(Long id) {
        super("The inspection with id " + id + " was not found");
    }
}
