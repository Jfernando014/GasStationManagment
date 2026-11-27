package com.edu.unicauca.gasstation.backend.appearance.exception;

/** Raised when a color is too light to remain legible on light surfaces. */
public class InsufficientColorContrastException extends RuntimeException {

    public InsufficientColorContrastException(String hex) {
        super("El color " + hex + " es demasiado claro para mantener contraste legible sobre fondo claro");
    }
}
