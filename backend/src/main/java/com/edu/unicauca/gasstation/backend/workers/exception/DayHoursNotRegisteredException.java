package com.edu.unicauca.gasstation.backend.workers.exception;

import java.time.LocalDate;

/**
 * The date has no processing state: its journal was never closed, or closed before the hours were kept.
 * Translated to 404.
 */
public class DayHoursNotRegisteredException extends RuntimeException {

    public DayHoursNotRegisteredException(LocalDate workDate) {
        super("No hay registro de horas trabajadas para la fecha " + workDate);
    }
}
