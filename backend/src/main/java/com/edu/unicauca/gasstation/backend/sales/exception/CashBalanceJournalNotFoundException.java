package com.edu.unicauca.gasstation.backend.sales.exception;

import java.time.LocalDate;

/** The date of the cash balance table has no journal. */
public class CashBalanceJournalNotFoundException extends RuntimeException {

    public CashBalanceJournalNotFoundException(LocalDate date) {
        super("No existe la jornada del " + date + ". Ábrela antes de registrar el saldo en caja.");
    }
}
