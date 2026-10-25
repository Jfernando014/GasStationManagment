package com.edu.unicauca.gasstation.backend.shifts.exception;

/**
 * The replacement already works that day a shift whose hours cross the hours of the shift to cover.
 * Translated to 409.
 */
public class ShiftHoursOverlapException extends RuntimeException {

    public ShiftHoursOverlapException(String fullName, String currentCode, String requestedCode) {
        super(fullName + " ya trabaja " + currentCode + " ese día y se cruza en horas con " + requestedCode);
    }
}
