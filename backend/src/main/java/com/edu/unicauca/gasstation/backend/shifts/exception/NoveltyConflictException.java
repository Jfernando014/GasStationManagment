package com.edu.unicauca.gasstation.backend.shifts.exception;

/**
 * The worker already takes part in another active novelty that day (RN-D4). Translated to 409.
 */
public class NoveltyConflictException extends RuntimeException {

    public NoveltyConflictException(String fullName) {
        super(fullName + " ya tiene una novedad ese día; anúlela antes de registrar otra");
    }
}
