package com.edu.unicauca.gasstation.backend.security;

import java.time.Instant;

/**
 * Example domain event for security module event bus.
 * Published across modules in modular monolith architecture.
 */
public record SecurityEventExample(Long entityId, String eventType, Instant occurredOn) {

    public static SecurityEventExample create(Long entityId, String eventType) {
        return new SecurityEventExample(entityId, eventType, Instant.now());
    }
}
