package com.edu.unicauca.gasstation.backend.sales.domain.services;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Exact representability in the provisional NUMERIC(30,12) storage format.
 * Does not alter the original amount or replace the nonnegative-value rule of SalesTarget.
 */
public final class SalesTargetStoragePolicy {

    private static final BigDecimal EXCLUSIVE_ABSOLUTE_LIMIT = new BigDecimal("1E18");
    private static final int STORAGE_SCALE = 12;

    private SalesTargetStoragePolicy() {
    }

    public static void validate(BigDecimal value) {
        Objects.requireNonNull(value, "value");
        if (value.abs().compareTo(EXCLUSIVE_ABSOLUTE_LIMIT) >= 0) {
            throw new IllegalArgumentException("absolute value must be less than 10^18");
        }
        try {
            // The result is intentionally discarded: validation must not normalize the domain amount.
            value.setScale(STORAGE_SCALE, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException ex) {
            throw new IllegalArgumentException("value must be exactly representable with 12 decimal places", ex);
        }
    }
}
