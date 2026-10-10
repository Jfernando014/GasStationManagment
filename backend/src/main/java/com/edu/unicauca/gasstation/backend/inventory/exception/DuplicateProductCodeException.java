package com.edu.unicauca.gasstation.backend.inventory.exception;

public class DuplicateProductCodeException extends RuntimeException {
    public DuplicateProductCodeException(String code) {
        super("Already exits a product with code:" + code);
    }
}
