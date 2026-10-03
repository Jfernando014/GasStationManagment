package com.edu.unicauca.gasstation.backend.sales;

import java.time.Instant;

/**
 * Example domain event for sales module event bus.
 * Published across modules in modular monolith architecture.
 */
public record SaleEventExample(Long entityId, String eventType, Instant occurredOn) {

    public static SaleEventExample create(Long entityId, String eventType) {
        return new SaleEventExample(entityId, eventType, Instant.now());
    }
}
