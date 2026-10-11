package com.edu.unicauca.gasstation.backend.shifts.exception;

import java.util.UUID;

/**
 * The worker to schedule is inactive. Translated to 409.
 */
public class WorkerNotAvailableException extends RuntimeException {

    public WorkerNotAvailableException(UUID workerId) {
        super("El vendedor con id " + workerId + " está inactivo y no se le pueden asignar turnos");
    }
}
