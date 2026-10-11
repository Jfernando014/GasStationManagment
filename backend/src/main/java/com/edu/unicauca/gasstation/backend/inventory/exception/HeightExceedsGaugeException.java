package com.edu.unicauca.gasstation.backend.inventory.exception;

import java.math.BigDecimal;

public class HeightExceedsGaugeException extends RuntimeException {
    private final BigDecimal maxHeightCm;
    public HeightExceedsGaugeException(BigDecimal maxHeightCm) {
        super("The entered measurement exceeds the maximum height of the tank ");
        this.maxHeightCm = maxHeightCm;
    }
    public BigDecimal getMaxHeightCm() {
        return maxHeightCm;
    }
}
