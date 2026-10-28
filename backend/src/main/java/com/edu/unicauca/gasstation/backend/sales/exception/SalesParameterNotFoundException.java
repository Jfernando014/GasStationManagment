package com.edu.unicauca.gasstation.backend.sales.exception;

/** No value of the parameter is effective on the requested date. */
public class SalesParameterNotFoundException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public SalesParameterNotFoundException(String message) {
        super(message);
    }
}
