package com.edu.unicauca.gasstation.backend.statistics.domain.models;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Immutable metadata of a journal announced as closed.
 * The operational date is supplied by the producer, never derived from the closing timestamp.
 * journalId may be absent: the public closing event also supports announcements identified by date.
 */
public record ClosedJournalSnapshot(
        Long journalId,
        LocalDate journalDate,
        Long consecutive,
        LocalDateTime closedAt) {

    public ClosedJournalSnapshot {
        Objects.requireNonNull(journalDate, "journalDate");
        Objects.requireNonNull(consecutive, "consecutive");
        Objects.requireNonNull(closedAt, "closedAt");
        if (journalId != null && journalId <= 0) {
            throw new IllegalArgumentException("journalId must be positive when present");
        }
        if (consecutive <= 0) {
            throw new IllegalArgumentException("consecutive must be positive");
        }
    }
}
