package com.edu.unicauca.gasstation.backend.inventory.exception;

public class ProductNotFoundException extends RuntimeException {
    public ProductNotFoundException(Long id) {
        super("The producto with ID: " + id + " was not found");
    }
}
