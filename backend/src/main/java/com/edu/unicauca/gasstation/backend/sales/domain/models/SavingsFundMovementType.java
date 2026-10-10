package com.edu.unicauca.gasstation.backend.sales.domain.models;

import java.math.BigDecimal;

/** Types of savings fund movements and how each one affects the balance. */
public enum SavingsFundMovementType {

    CONTRIBUTION(1),
    PAYMENT(-1);

    private final int sign;

    SavingsFundMovementType(int sign) {
        this.sign = sign;
    }

    public BigDecimal signed(BigDecimal amount) {
        return amount.multiply(BigDecimal.valueOf(sign));
    }
}
