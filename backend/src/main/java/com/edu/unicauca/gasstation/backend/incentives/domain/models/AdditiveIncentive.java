package com.edu.unicauca.gasstation.backend.incentives.domain.models;

/** Row of the additive list: the rule applying on a date (null if none) and the next scheduled one (null if none). */
public record AdditiveIncentive(Long productId, String productName, IncentiveRule current, IncentiveRule next,
                                boolean hasRules) {
}
