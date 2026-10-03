package com.edu.unicauca.gasstation.backend.reports;

import java.time.Instant;

/**
 * Example domain event for reports module event bus.
 * Published across modules in modular monolith architecture.
 */
public record ReportEventExample(Long entityId, String eventType, Instant occurredOn) {

    public static ReportEventExample create(Long entityId, String eventType) {
        return new ReportEventExample(entityId, eventType, Instant.now());
    }
}
