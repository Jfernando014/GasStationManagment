package com.edu.unicauca.gasstation.backend.workers.exception;

/** The fortnight asked for is not 1 or 2 of a month. Translated to 400. */
public class InvalidFortnightException extends RuntimeException {

    public InvalidFortnightException(String message) {
        super(message);
    }
}
