package com.edu.unicauca.gasstation.backend.workers.exception;

import java.time.LocalDate;

/** Keeping the hours of a date failed; the date was left pending. Translated to 500. */
public class DayHoursProcessingException extends RuntimeException {

    public DayHoursProcessingException(LocalDate workDate) {
        super("No se pudieron procesar las horas trabajadas del " + workDate
                + "; la fecha quedó pendiente. Intente reprocesarla de nuevo");
    }
}
