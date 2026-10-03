package com.edu.unicauca.gasstation.backend.statistics;

import java.time.Instant;

/**
 * Example domain event for statistics module event bus.
 * Published across modules in modular monolith architecture.
 */
public record StatisticsEventExample(Long entityId, String eventType, Instant occurredOn) {

    public static StatisticsEventExample create(Long entityId, String eventType) {
        return new StatisticsEventExample(entityId, eventType, Instant.now());
    }
}
