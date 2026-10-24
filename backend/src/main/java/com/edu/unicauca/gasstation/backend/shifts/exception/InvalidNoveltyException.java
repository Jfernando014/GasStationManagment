package com.edu.unicauca.gasstation.backend.shifts.exception;

/**
 * The novelty does not make sense for the staff of the day (for example, the absence of a worker who does not work
 * that day). Translated to 400.
 */
public class InvalidNoveltyException extends RuntimeException {

    public InvalidNoveltyException(String detail) {
        super(detail);
    }
}
