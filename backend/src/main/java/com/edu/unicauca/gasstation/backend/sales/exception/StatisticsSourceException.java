package com.edu.unicauca.gasstation.backend.sales.exception;

/** A connected source violated its completeness/identity/range contract. */
public class StatisticsSourceException extends RuntimeException {
    public StatisticsSourceException(String message) { super(message); }
    public StatisticsSourceException(String message, Throwable cause) { super(message, cause); }
}
