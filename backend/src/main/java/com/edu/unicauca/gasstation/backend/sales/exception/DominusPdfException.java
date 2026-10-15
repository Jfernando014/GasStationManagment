package com.edu.unicauca.gasstation.backend.sales.exception;

public class DominusPdfException extends RuntimeException {
    public enum Reason { EMPTY, INVALID_PDF, UNSUPPORTED_TYPE, TOO_LARGE, INVALID_DATA, LAYOUT_OVERFLOW, GENERATION_FAILED }

    private final Reason reason;

    public DominusPdfException(Reason reason, String message) {
        super(message);
        this.reason = reason;
    }

    public DominusPdfException(Reason reason, String message, Throwable cause) {
        super(message, cause);
        this.reason = reason;
    }

    public Reason getReason() {
        return reason;
    }
}
