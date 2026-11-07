package com.edu.unicauca.gasstation.backend.sales.exception;

import java.util.List;

/** Step 4 cannot be completed yet: some figure of the cash balance table is missing. */
public class CashBalanceNotReadyException extends RuntimeException {

    private final List<String> missing;

    public CashBalanceNotReadyException(List<String> missing) {
        super("El paso 4 no se puede completar todavía. " + String.join(" ", missing));
        this.missing = List.copyOf(missing);
    }

    /** What is still missing, one sentence each, ready to show to the user. */
    public List<String> getMissing() {
        return missing;
    }
}
