package com.edu.unicauca.gasstation.backend.inventory.exception;

public class InvalidInventoryAlertException extends RuntimeException {

    public InvalidInventoryAlertException(String message) {
        super(message);
    }
}