package com.edu.unicauca.gasstation.backend.workers.exception;

/** The manual adjustment does not exist. Translated to 404. */
public class AdjustmentNotFoundException extends RuntimeException {

    public AdjustmentNotFoundException(Long id) {
        super("No se encontró el ajuste con id " + id);
    }
}
