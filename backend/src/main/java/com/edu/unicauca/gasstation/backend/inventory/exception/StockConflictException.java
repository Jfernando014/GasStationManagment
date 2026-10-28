package com.edu.unicauca.gasstation.backend.inventory.exception;

public class StockConflictException extends RuntimeException {

    public StockConflictException(String message) {
        super(message);
    }
}