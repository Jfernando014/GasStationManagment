package com.edu.unicauca.gasstation.backend.workers.domain.models;

/** Whether a manual adjustment adds hours to a category or takes them away. */
public enum AdjustmentDirection {
    /** "Más": adds hours. */
    ADD(1),
    /** "Menos": subtracts hours. */
    SUBTRACT(-1);

    private final int sign;

    AdjustmentDirection(int sign) {
        this.sign = sign;
    }

    public int sign() {
        return sign;
    }
}
