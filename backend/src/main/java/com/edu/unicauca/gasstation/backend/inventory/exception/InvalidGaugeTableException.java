package com.edu.unicauca.gasstation.backend.inventory.exception;

public class InvalidGaugeTableException extends RuntimeException {
    private final String field;
    private final String detail;

    public InvalidGaugeTableException(String field, String detail) {
        super(detail == null || detail.isBlank() ? "Please correct the height and volume values" : detail);
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
