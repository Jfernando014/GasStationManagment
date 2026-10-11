package com.edu.unicauca.gasstation.backend.inventory.exception;

public class InvalidGaugeTableException extends RuntimeException {
    private final String field;
    private final String detail;
    public InvalidGaugeTableException(String field, String detail) {
        super("Please correct the height and volume values");
        this.field = field;
        this.detail = detail;
    }
    public String getField() {
        return field;
    }
    public String getDetail() {
        return detail;
    }
}
