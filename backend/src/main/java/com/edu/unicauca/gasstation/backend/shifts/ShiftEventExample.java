package com.edu.unicauca.gasstation.backend.shifts;

import java.time.Instant;

/**
 * Example domain event for shifts module event bus.
 * Published across modules in modular monolith architecture.
 */
public record ShiftEventExample(Long entityId, String eventType, Instant occurredOn) {

    public static ShiftEventExample create(Long entityId, String eventType) {
        return new ShiftEventExample(entityId, eventType, Instant.now());
    }
}
