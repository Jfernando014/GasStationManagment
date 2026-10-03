package com.edu.unicauca.gasstation.backend.shifts.exception;

/**
 * Example custom domain exception for shifts module.
 */
public class ShiftNotFoundExceptionExample extends RuntimeException {

    public ShiftNotFoundExceptionExample(String message) {
        super(message);
    }

    public ShiftNotFoundExceptionExample(Long id) {
        super("Shift with ID " + id + " not found");
    }
}
