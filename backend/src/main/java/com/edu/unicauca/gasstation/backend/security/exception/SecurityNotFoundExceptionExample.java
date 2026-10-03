package com.edu.unicauca.gasstation.backend.security.exception;

/**
 * Example custom domain exception for security module.
 */
public class SecurityNotFoundExceptionExample extends RuntimeException {

    public SecurityNotFoundExceptionExample(String message) {
        super(message);
    }

    public SecurityNotFoundExceptionExample(Long id) {
        super("Security with ID " + id + " not found");
    }
}
