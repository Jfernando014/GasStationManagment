package com.edu.unicauca.gasstation.backend.sales.exception;

import java.time.LocalDate;

/** Thrown when a daily contribution already exists for a date. */
public class DuplicateContributionException extends RuntimeException {

    public DuplicateContributionException(LocalDate date) {
        super("A contribution for " + date + " is already registered");
    }
}
