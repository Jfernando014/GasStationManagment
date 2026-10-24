package com.edu.unicauca.gasstation.backend.shifts.exception;

/**
 * The reason of a novelty is missing or does not have 3 to 255 characters without leading and trailing spaces
 * (VAL-D6). Translated to 400.
 */
public class InvalidNoveltyReasonException extends RuntimeException {

    public InvalidNoveltyReasonException() {
        super("El motivo es obligatorio y debe tener entre 3 y 255 caracteres");
    }
}
