package com.edu.unicauca.gasstation.backend.statistics.exception;

/** A failed or invalid source must never become a successful empty performance report. */
public class SellerPerformanceSourceException extends RuntimeException {
    public SellerPerformanceSourceException(RuntimeException cause) {
        super("Performance sources could not be read or validated", cause);
    }
}
