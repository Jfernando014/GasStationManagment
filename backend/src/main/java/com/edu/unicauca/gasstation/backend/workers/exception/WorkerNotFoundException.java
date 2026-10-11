package com.edu.unicauca.gasstation.backend.workers.exception;

import java.util.UUID;

/**
 * The requested worker does not exist. Translated to 404.
 */
public class WorkerNotFoundException extends RuntimeException {

    public WorkerNotFoundException(UUID id) {
        super("No se encontró el trabajador con id " + id);
    }
}
