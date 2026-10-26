package com.edu.unicauca.gasstation.backend.sales.domain.reconciliation;

import java.math.BigDecimal;

/** Shared validation of COP amounts: non-negative, at most 2 decimals, at most 12 integer digits. Blank means zero. */
final class Money {
    static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2);

    private Money() {
    }

    static BigDecimal amount(String field, BigDecimal value) {
        if (value == null) {
            return ZERO;
        }
        if (value.signum() < 0) {
            throw new ReconciliationValidationException(field, "El valor de '" + field + "' no puede ser negativo");
        }
        BigDecimal stripped = value.stripTrailingZeros();
        if (stripped.scale() > 2) {
            throw new ReconciliationValidationException(field,
                    "El valor de '" + field + "' no puede tener más de 2 decimales");
        }
        if (stripped.precision() - stripped.scale() > 12) {
            throw new ReconciliationValidationException(field, "El valor de '" + field + "' es demasiado grande");
        }
        return value.setScale(2);
    }
}
