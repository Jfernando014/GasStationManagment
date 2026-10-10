package com.edu.unicauca.gasstation.backend.shifts.exception;

import java.util.UUID;

/**
 * The worker to schedule does not exist. Translated to 404.
 */
public class ScheduleWorkerNotFoundException extends RuntimeException {

    public ScheduleWorkerNotFoundException(UUID workerId) {
        super("No se encontró el vendedor con id " + workerId);
    }
}
