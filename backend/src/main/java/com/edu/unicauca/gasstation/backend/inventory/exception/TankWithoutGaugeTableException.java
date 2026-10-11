package com.edu.unicauca.gasstation.backend.inventory.exception;

public class TankWithoutGaugeTableException extends RuntimeException {
    public TankWithoutGaugeTableException(Long tankId) {
        super("This tank does not have a gauge table configured.");
    }
}
