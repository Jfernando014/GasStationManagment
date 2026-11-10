package com.edu.unicauca.gasstation.backend.statistics.exception;

import java.util.Objects;

/** An unresolved source record prevents confirmation of the whole day's historical snapshot. */
public final class ShiftSaleHistoryIntegrationException extends RuntimeException {
    private final Code code;

    public ShiftSaleHistoryIntegrationException(Code code, String detail) {
        this(code, detail, null);
    }

    public ShiftSaleHistoryIntegrationException(Code code, String detail, Throwable cause) {
        super(Objects.requireNonNull(code, "code").name() + ": " + detail, cause);
        this.code = code;
    }

    public Code code() {
        return code;
    }

    public enum Code {
        INVALID_SALES_RESPONSE,
        INVALID_STAFF_RESPONSE,
        STAFF_NOT_CONFIRMED,
        INVALID_VENDOR_IDENTIFIER,
        WORKER_NOT_IN_DAY_STAFF,
        SHIFT_MISMATCH,
        AMBIGUOUS_SHIFT,
        UNSUPPORTED_ROLE,
        INCONSISTENT_SUPPORT,
        DUPLICATE_CLOSE,
        INVALID_SALE
    }
}
