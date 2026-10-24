package com.edu.unicauca.gasstation.backend.shifts.exception;

/**
 * The replacement already works another shift that day, so the replacement would give them two codes (RN-D4).
 * Translated to 409.
 */
public class WorkerAlreadyWorkingException extends RuntimeException {

    public WorkerAlreadyWorkingException(String fullName, String currentCode) {
        super(fullName + " ya trabaja " + currentCode + " ese día; un vendedor tiene a lo sumo un código por día");
    }
}
