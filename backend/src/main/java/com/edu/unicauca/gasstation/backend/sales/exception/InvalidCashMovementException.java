package com.edu.unicauca.gasstation.backend.sales.exception;

/** Thrown when a cash movement has an invalid amount for its type. */
public class InvalidCashMovementException extends RuntimeException {

    public InvalidCashMovementException(String message) {
        super(message);
    }
}
