package com.edu.unicauca.gasstation.backend.shifts.exception;

import java.time.LocalDate;

/**
 * There is no journal for the date of step 1 (VAL-D1). Translated to 404.
 */
public class JournalNotFoundException extends RuntimeException {

    public JournalNotFoundException(LocalDate date) {
        super("No existe una jornada para la fecha " + date);
    }
}
