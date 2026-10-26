package com.edu.unicauca.gasstation.backend.inventory.exception;

import java.time.LocalDate;

public class NoSalesDataException extends RuntimeException {

    public NoSalesDataException(LocalDate journalDate) {
        super("There are no sales for the journal " + journalDate);
    }
}