package com.edu.unicauca.gasstation.backend.sales.exception;

import java.time.LocalDate;

/** Thrown when a daily contribution already exists for a date. */
public class DuplicateContributionException extends RuntimeException {

    public DuplicateContributionException(LocalDate date) {
        super("Ya existe un aporte registrado para la fecha " + date);
    }
}
