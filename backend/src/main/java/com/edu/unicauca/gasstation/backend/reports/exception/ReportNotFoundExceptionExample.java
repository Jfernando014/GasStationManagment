package com.edu.unicauca.gasstation.backend.reports.exception;

/**
 * Example custom domain exception for reports module.
 */
public class ReportNotFoundExceptionExample extends RuntimeException {

    public ReportNotFoundExceptionExample(String message) {
        super(message);
    }

    public ReportNotFoundExceptionExample(Long id) {
        super("Report with ID " + id + " not found");
    }
}
