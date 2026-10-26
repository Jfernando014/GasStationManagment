package com.edu.unicauca.gasstation.backend.incentives.domain.models;

import java.time.Instant;
import java.time.LocalDate;

/** Pesos earned per unit sold of an additive from {@code validFrom}. Never edited: a change is a new rule. */
public record IncentiveRule(Long id, Long productId, int valuePerUnit, LocalDate validFrom, boolean active,
                            String reason, Instant createdAt) {

    /** Highest value per unit allowed; the database check uses the same limit. */
    public static final int MAX_VALUE_PER_UNIT = 100_000;

    public static IncentiveRule create(Long productId, int valuePerUnit, LocalDate validFrom, String reason,
                                       Instant createdAt) {
        return new IncentiveRule(null, productId, valuePerUnit, validFrom, true, reason, createdAt);
    }
}
