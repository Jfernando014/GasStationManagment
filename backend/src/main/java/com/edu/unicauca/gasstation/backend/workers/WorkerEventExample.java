package com.edu.unicauca.gasstation.backend.workers;

import java.time.Instant;

/**
 * Example domain event for workers module event bus.
 * Published across modules in modular monolith architecture.
 */
public record WorkerEventExample(Long entityId, String eventType, Instant occurredOn) {

    public static WorkerEventExample create(Long entityId, String eventType) {
        return new WorkerEventExample(entityId, eventType, Instant.now());
    }
}
