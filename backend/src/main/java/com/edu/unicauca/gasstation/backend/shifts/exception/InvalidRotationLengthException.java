package com.edu.unicauca.gasstation.backend.shifts.exception;

import java.time.LocalDate;

/**
 * The rotation range is shorter than one full cycle or longer than a quarter. Translated to 400.
 */
public class InvalidRotationLengthException extends RuntimeException {

    public InvalidRotationLengthException(LocalDate startDate, LocalDate minEndDate, LocalDate maxEndDate) {
        super("La rotación debe cubrir al menos un ciclo completo y como máximo un trimestre: si inicia el "
                + startDate + ", la fecha final debe estar entre " + minEndDate + " y " + maxEndDate);
    }
}
