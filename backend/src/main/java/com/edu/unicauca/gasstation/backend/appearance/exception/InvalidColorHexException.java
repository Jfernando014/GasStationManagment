package com.edu.unicauca.gasstation.backend.appearance.exception;

/** Raised when a submitted color is not a valid 6-digit hexadecimal value ({@code #RRGGBB}). */
public class InvalidColorHexException extends RuntimeException {

    public InvalidColorHexException(String rawValue) {
        super("El color '" + rawValue + "' no es un código hexadecimal válido (#RRGGBB)");
    }
}
