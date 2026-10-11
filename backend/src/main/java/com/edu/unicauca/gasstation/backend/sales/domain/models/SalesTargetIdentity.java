package com.edu.unicauca.gasstation.backend.sales.domain.models;

import java.util.Objects;

/**
 * Conceptual identity of a configured aggregate target: its period and category.
 * This is not a database identifier or a final persistence uniqueness contract.
 * Calculated individual targets and fuel-product breakdowns are outside this model.
 */
public record SalesTargetIdentity(SalesTargetPeriod period, SalesTargetCategory category) {

    public SalesTargetIdentity {
        Objects.requireNonNull(period, "period");
        Objects.requireNonNull(category, "category");
    }
}
