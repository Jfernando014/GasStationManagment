package com.edu.unicauca.gasstation.backend.reports.exception;

/**
 * Thrown when the requested report period is invalid.
 *
 * @author javiersolanop777
 */
public class InvalidReportPeriodException extends RuntimeException {

    public InvalidReportPeriodException(String message) {
        super(message);
    }
}
