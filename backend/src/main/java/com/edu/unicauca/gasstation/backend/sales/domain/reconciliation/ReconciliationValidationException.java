package com.edu.unicauca.gasstation.backend.sales.domain.reconciliation;

/** Invalid value in a reconciliation input. Carries the offending field so the UI can point at it (HU-34 #5). */
public class ReconciliationValidationException extends IllegalArgumentException {
    private static final long serialVersionUID = 1L;

    private final String field;

    public ReconciliationValidationException(String field, String message) {
        super(message);
        this.field = field;
    }

    public String field() {
        return field;
    }
}
