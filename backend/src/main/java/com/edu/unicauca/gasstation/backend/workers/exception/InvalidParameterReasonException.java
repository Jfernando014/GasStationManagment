package com.edu.unicauca.gasstation.backend.workers.exception;

/** The reason of a new version of the overtime parameters is shorter or longer than allowed. Translated to 400. */
public class InvalidParameterReasonException extends RuntimeException {

    public InvalidParameterReasonException(int min, int max) {
        super("El motivo debe tener entre " + min + " y " + max + " caracteres");
    }
}
