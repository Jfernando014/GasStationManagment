package com.edu.unicauca.gasstation.backend.workers.exception;

/** The manual adjustment has zero or negative hours, an unknown category or direction, or a bad reason. Translated to 400. */
public class InvalidAdjustmentException extends RuntimeException {

    public InvalidAdjustmentException(String message) {
        super(message);
    }
}
