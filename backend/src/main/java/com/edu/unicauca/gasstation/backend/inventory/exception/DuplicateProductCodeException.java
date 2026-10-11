package com.edu.unicauca.gasstation.backend.inventory.exception;

public class DuplicateProductCodeException extends RuntimeException {
    public DuplicateProductCodeException(String code) {
        super("Ya existe un producto con el código " + code);
    }
}
