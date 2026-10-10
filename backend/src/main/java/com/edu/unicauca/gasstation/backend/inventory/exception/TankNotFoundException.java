package com.edu.unicauca.gasstation.backend.inventory.exception;

public class TankNotFoundException extends RuntimeException {
    public TankNotFoundException(Long id) {
        super("Tank with code " + id + " not found");
    }
}
