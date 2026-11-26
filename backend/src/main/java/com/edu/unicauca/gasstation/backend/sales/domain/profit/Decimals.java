package com.edu.unicauca.gasstation.backend.sales.domain.profit;

import java.math.BigDecimal;

/** Presentation of exact decimals: no trailing zeros beyond the two decimals of money. */
final class Decimals {

    private Decimals() {
    }

    static BigDecimal plain(BigDecimal value) {
        if (value == null) {
            return null;
        }
        BigDecimal stripped = value.stripTrailingZeros();
        return stripped.scale() < 2 ? stripped.setScale(2) : stripped;
    }
}
