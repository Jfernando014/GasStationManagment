package com.edu.unicauca.gasstation.backend.workers.exception;

import java.time.LocalDate;

/** The validity of a new version of the overtime parameters is not after the last day with hours kept. Translated to 409. */
public class ParameterValidFromException extends RuntimeException {

    public ParameterValidFromException(LocalDate lastRegisteredDate) {
        super("La vigencia debe ser posterior a la última fecha con horas registradas (" + lastRegisteredDate + ")");
    }
}
