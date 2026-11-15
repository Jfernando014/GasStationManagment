package com.edu.unicauca.gasstation.backend.reports.exception;

/**
 * Thrown when a report has no source data to render.
 *
 * @author javiersolanop777
 */
public class ReportNoDataException extends RuntimeException {

    public ReportNoDataException(String message) {
        super(message);
    }
}
