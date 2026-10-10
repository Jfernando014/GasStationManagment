package com.edu.unicauca.gasstation.backend.shifts.exception;

import java.time.LocalDate;

/**
 * The end of a date range is before its start. Translated to 400.
 */
public class InvalidDateRangeException extends RuntimeException {

    public InvalidDateRangeException(LocalDate start, LocalDate end) {
        super("La fecha final (" + end + ") no puede ser anterior a la fecha inicial (" + start + ")");
    }
}
