package com.edu.unicauca.gasstation.backend.sales.exception;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Thrown when a payment is greater than the fund balance at its date. */
public class InsufficientFundException extends RuntimeException {

    public InsufficientFundException(BigDecimal available, BigDecimal requested, LocalDate date) {
        super("Fondo de ahorro insuficiente para la fecha " + date + ": disponible " + available
                + ", solicitado " + requested);
    }

    public InsufficientFundException(LocalDate date, BigDecimal balance) {
        super("Fondo de ahorro insuficiente: el saldo quedaría en negativo en la fecha " + date + ": " + balance);
    }
}
