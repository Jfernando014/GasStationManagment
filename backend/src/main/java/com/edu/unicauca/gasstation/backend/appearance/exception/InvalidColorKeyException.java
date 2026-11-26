package com.edu.unicauca.gasstation.backend.appearance.exception;

import com.edu.unicauca.gasstation.backend.appearance.domain.models.ColorCategory;

/** Raised when a color key does not match any supported fuel, shift family, or worker UUID. */
public class InvalidColorKeyException extends RuntimeException {

    public InvalidColorKeyException(ColorCategory category, String key) {
        super("La clave '" + key + "' no es válida para la categoría " + category);
    }
}
