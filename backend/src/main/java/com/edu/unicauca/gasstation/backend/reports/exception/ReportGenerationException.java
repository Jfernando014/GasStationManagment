package com.edu.unicauca.gasstation.backend.reports.exception;

/**
 * Thrown when a report file cannot be generated.
 *
 * @author javiersolanop777
 */
public class ReportGenerationException extends RuntimeException {

    public ReportGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}
