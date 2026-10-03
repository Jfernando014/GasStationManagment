package com.edu.unicauca.gasstation.backend.incentives;

import java.time.Instant;

/**
 * Example domain event for incentives module event bus.
 * Published across modules in modular monolith architecture.
 */
public record IncentiveEventExample(Long entityId, String eventType, Instant occurredOn) {

    public static IncentiveEventExample create(Long entityId, String eventType) {
        return new IncentiveEventExample(entityId, eventType, Instant.now());
    }
}
