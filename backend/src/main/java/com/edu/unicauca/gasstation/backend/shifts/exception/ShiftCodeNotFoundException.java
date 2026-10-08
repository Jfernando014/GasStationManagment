package com.edu.unicauca.gasstation.backend.shifts.exception;

public class ShiftCodeNotFoundException extends RuntimeException {

    public ShiftCodeNotFoundException(String code) {
        super("Shift code '" + code + "' not found");
    }
}
