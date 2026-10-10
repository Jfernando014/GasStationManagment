package com.edu.unicauca.gasstation.backend.sales.domain.models;

import java.math.BigDecimal;

/** Types of cash movements and how each one affects the cash balance. */
public enum CashMovementType {

    INITIAL_BALANCE(1),
    DEPOSIT(-1),
    ADJUSTMENT(1);

    private final int sign;

    CashMovementType(int sign) {
        this.sign = sign;
    }

    public BigDecimal signed(BigDecimal amount) {
        return amount.multiply(BigDecimal.valueOf(sign));
    }

    public boolean allowsNegativeAmount() {
        return this == ADJUSTMENT;
    }
}
