package com.edu.unicauca.gasstation.backend.inventory.exception;

public class ProductNotFoundException extends RuntimeException {
    public ProductNotFoundException(Long id) {
        super("No se encontró el producto con id " + id);
    }
}
