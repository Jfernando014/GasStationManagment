package com.edu.unicauca.gasstation.backend.shifts.exception;

import java.time.LocalDate;

/**
 * A shift was scheduled on a day before today (station time zone). Translated to 400.
 */
public class PastDateException extends RuntimeException {

    public PastDateException(LocalDate date) {
        super("La fecha " + date + " ya pasó; solo se pueden programar turnos desde hoy");
    }
}
