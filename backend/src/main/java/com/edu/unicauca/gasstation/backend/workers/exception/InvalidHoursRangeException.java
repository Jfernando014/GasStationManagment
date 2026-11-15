package com.edu.unicauca.gasstation.backend.workers.exception;

/** The range of dates asked for is reversed or longer than allowed. Translated to 400. */
public class InvalidHoursRangeException extends RuntimeException {

    public InvalidHoursRangeException(String message) {
        super(message);
    }
}
