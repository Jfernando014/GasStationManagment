package com.edu.unicauca.gasstation.backend.incentives.exception;

/** Error of the incentives module; {@link IncentiveError} gives its code, status and message. */
public class IncentiveException extends RuntimeException {

    private final IncentiveError error;

    /** @param args values for the placeholders of the message, in order */
    public IncentiveException(IncentiveError error, Object... args) {
        super(error.message(args));
        this.error = error;
    }

    public IncentiveError error() {
        return error;
    }
}
