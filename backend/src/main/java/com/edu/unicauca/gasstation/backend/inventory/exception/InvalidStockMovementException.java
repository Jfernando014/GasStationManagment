package com.edu.unicauca.gasstation.backend.inventory.exception;

public class InvalidStockMovementException extends RuntimeException {

    public InvalidStockMovementException(String message) {
        super(message);
    }
}