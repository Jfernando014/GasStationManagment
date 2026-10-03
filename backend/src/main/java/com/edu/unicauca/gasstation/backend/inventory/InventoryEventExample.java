package com.edu.unicauca.gasstation.backend.inventory;

import java.time.Instant;

/**
 * Example domain event for inventory module event bus.
 * Published across modules in modular monolith architecture.
 */
public record InventoryEventExample(Long entityId, String eventType, Instant occurredOn) {

    public static InventoryEventExample create(Long entityId, String eventType) {
        return new InventoryEventExample(entityId, eventType, Instant.now());
    }
}
