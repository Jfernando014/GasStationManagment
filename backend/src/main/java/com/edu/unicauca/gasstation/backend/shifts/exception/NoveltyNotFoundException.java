package com.edu.unicauca.gasstation.backend.shifts.exception;

import java.util.UUID;

/**
 * The novelty does not exist, or belongs to another date. Translated to 404.
 */
public class NoveltyNotFoundException extends RuntimeException {

    public NoveltyNotFoundException(UUID noveltyId) {
        super("No se encontró la novedad con id " + noveltyId);
    }
}
