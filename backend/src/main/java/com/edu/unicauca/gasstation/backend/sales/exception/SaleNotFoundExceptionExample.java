package com.edu.unicauca.gasstation.backend.sales.exception;

/**
 * Example custom domain exception for sales module.
 */
public class SaleNotFoundExceptionExample extends RuntimeException {

    public SaleNotFoundExceptionExample(String message) {
        super(message);
    }

    public SaleNotFoundExceptionExample(Long id) {
        super("Sale with ID " + id + " not found");
    }
}
