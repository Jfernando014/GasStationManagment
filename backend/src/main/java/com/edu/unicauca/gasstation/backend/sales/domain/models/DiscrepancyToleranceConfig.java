package com.edu.unicauca.gasstation.backend.sales.domain.models;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Domain model representing vendor discrepancy tolerance policy with effective date.
 * Strictly enforces non-negative integer/decimal amount and effective date.
 */
public record DiscrepancyToleranceConfig(
        Long id,
        BigDecimal toleranceAmount,
        LocalDate effectiveDate,
        String notes,
        Instant createdAt,
        String createdBy
) {
    public DiscrepancyToleranceConfig {
        Objects.requireNonNull(toleranceAmount, "Tolerance amount must not be null");
        Objects.requireNonNull(effectiveDate, "Effective date must not be null");
        if (toleranceAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Tolerance amount cannot be negative");
        }
    }

    public static DiscrepancyToleranceConfig create(BigDecimal amount, LocalDate effectiveDate, String notes, String user) {
        return new DiscrepancyToleranceConfig(null, amount, effectiveDate, notes, Instant.now(), user != null ? user : "SYSTEM");
    }
}
