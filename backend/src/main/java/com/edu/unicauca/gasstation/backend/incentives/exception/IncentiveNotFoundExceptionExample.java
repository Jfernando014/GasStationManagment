package com.edu.unicauca.gasstation.backend.incentives.exception;

/**
 * Example custom domain exception for incentives module.
 */
public class IncentiveNotFoundExceptionExample extends RuntimeException {

    public IncentiveNotFoundExceptionExample(String message) {
        super(message);
    }

    public IncentiveNotFoundExceptionExample(Long id) {
        super("Incentive with ID " + id + " not found");
    }
}
