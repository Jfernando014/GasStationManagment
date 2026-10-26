package com.edu.unicauca.gasstation.backend.security.exception;

import java.time.LocalDateTime;
import lombok.Getter;

@Getter
public class AccountLockedException extends RuntimeException {

    private final LocalDateTime lockedUntil;
    private final long minutesRemaining;

    public AccountLockedException(String message, LocalDateTime lockedUntil, long minutesRemaining) {
        super(message);
        this.lockedUntil = lockedUntil;
        this.minutesRemaining = minutesRemaining;
    }
}
