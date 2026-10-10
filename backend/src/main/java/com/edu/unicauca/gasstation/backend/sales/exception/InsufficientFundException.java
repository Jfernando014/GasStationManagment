package com.edu.unicauca.gasstation.backend.sales.exception;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Thrown when a payment is greater than the fund balance at its date. */
public class InsufficientFundException extends RuntimeException {

    public InsufficientFundException(BigDecimal available, BigDecimal requested, LocalDate date) {
        super("Insufficient savings fund at " + date + ": available " + available
                + ", requested " + requested);
    }

    public InsufficientFundException(LocalDate date, BigDecimal balance) {
        super("Insufficient savings fund: balance would become negative at " + date + ": " + balance);
    }
}
