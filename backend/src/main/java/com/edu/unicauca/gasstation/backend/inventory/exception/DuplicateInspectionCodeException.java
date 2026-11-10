package com.edu.unicauca.gasstation.backend.inventory.exception;

public class DuplicateInspectionCodeException extends RuntimeException {

    public DuplicateInspectionCodeException(String code) {
        super("An inspection with code " + code + " already exists");
    }
}
