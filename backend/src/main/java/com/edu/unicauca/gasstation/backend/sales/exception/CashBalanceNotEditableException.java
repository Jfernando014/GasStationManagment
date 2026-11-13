package com.edu.unicauca.gasstation.backend.sales.exception;

/** The Dominus figures cannot be changed because the journal is closed or step 4 is already completed. */
public class CashBalanceNotEditableException extends RuntimeException {

    public CashBalanceNotEditableException(String message) {
        super(message);
    }
}
