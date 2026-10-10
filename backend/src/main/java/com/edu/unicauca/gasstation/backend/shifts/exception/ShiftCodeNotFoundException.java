package com.edu.unicauca.gasstation.backend.shifts.exception;

/**
 * The shift code does not exist, or is not active when it is being assigned. Translated to 404.
 */
public class ShiftCodeNotFoundException extends RuntimeException {

    public ShiftCodeNotFoundException(String code) {
        super("No se encontró el código de turno '" + code + "'");
    }
}
