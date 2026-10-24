package com.edu.unicauca.gasstation.backend.shifts.exception;

import java.time.LocalDate;

/**
 * The journal of the date is closed, so its step 1 is read only (VAL-D1, RN-D5). Translated to 409.
 */
public class JournalNotEditableException extends RuntimeException {

    public JournalNotEditableException(LocalDate date) {
        super("La jornada del " + date + " está cerrada; el personal del día ya no se puede cambiar");
    }
}
