package com.edu.unicauca.gasstation.backend.inventory.exception;

public class DuplicateTankCodeException extends RuntimeException {
    public DuplicateTankCodeException(String code) {
        super("A tank with code " + code + " already exists");
    }
}
