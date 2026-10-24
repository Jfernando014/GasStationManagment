package com.edu.unicauca.gasstation.backend.inventory.exception;

public class FuelReceptionNotFoundException extends RuntimeException {

    public FuelReceptionNotFoundException(Long id) {
        super("The fuel reception with id " + id + " was not found");
    }
}