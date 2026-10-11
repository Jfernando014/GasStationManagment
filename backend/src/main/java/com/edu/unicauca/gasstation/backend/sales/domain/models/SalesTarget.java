package com.edu.unicauca.gasstation.backend.sales.domain.models;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Immutable configured target, independent of persistence and individual target calculations.
 * Preserves the supplied decimal value and scale without rounding or unit conversion.
 * Zero is accepted while its business rule remains pending.
 */
public record SalesTarget(SalesTargetIdentity identity, BigDecimal value, SalesTargetUnit unit) {

    public SalesTarget {
        Objects.requireNonNull(identity, "identity");
        Objects.requireNonNull(value, "value");
        Objects.requireNonNull(unit, "unit");
        if (value.signum() < 0) {
            throw new IllegalArgumentException("value must not be negative");
        }

        SalesTargetUnit expectedUnit = switch (identity.category()) {
            case FUEL_AGGREGATE -> SalesTargetUnit.GALLONS;
            case BASKET -> SalesTargetUnit.COP;
        };
        if (unit != expectedUnit) {
            throw new IllegalArgumentException("unit must be " + expectedUnit + " for " + identity.category());
        }
    }
}
