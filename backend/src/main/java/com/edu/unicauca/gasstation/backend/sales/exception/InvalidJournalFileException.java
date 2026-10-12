package com.edu.unicauca.gasstation.backend.sales.exception;

public class InvalidJournalFileException extends RuntimeException {

    private final Reason reason;

    public InvalidJournalFileException(Reason reason, String message) {
        super(message);
        this.reason = reason;
    }

    public Reason getReason() {
        return reason;
    }

    public enum Reason {
        EMPTY,
        INVALID_NAME,
        UNSUPPORTED_TYPE,
        TOO_LARGE
    }
}