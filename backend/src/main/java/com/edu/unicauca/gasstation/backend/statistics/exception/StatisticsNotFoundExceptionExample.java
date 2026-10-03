package com.edu.unicauca.gasstation.backend.statistics.exception;

/**
 * Example custom domain exception for statistics module.
 */
public class StatisticsNotFoundExceptionExample extends RuntimeException {

    public StatisticsNotFoundExceptionExample(String message) {
        super(message);
    }

    public StatisticsNotFoundExceptionExample(Long id) {
        super("Statistics with ID " + id + " not found");
    }
}
