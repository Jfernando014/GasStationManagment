package com.edu.unicauca.gasstation.backend.reports.exception;

import java.time.LocalDate;

/**
 * Thrown when there is no cash balance table for the journal of a date.
 *
 * @author javiersolanop777
 */
public class CashBalanceNotFoundException extends RuntimeException {

    private final LocalDate atrDate;

    public CashBalanceNotFoundException(LocalDate prmDate) {
        super("There is no cash balance for the journal of " + prmDate);
        this.atrDate = prmDate;
    }

    public LocalDate getDate() {
        return atrDate;
    }
}
