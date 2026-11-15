package com.edu.unicauca.gasstation.backend.workers.domain.models;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Processing state of the hours of one date. {@code processedAt} is the last time the date was stored successfully,
 * so a date that failed again keeps it. {@code noveltiesApplied} is false when the staff of the day was read before
 * its novelties could be applied; a failed attempt leaves it as it was.
 */
public record HoursSync(LocalDate workDate, Long journalId, Long consecutive, LocalDateTime closedAt,
                        SyncStatus status, boolean noveltiesApplied, int attempts, String lastError,
                        Instant processedAt, Instant updatedAt) {

    /** Longest error text that is kept. */
    public static final int MAX_ERROR_LENGTH = 500;

    /** State after storing the date; clears the error. */
    public static HoursSync processed(HoursSync previous, JournalClosure closure, boolean noveltiesApplied,
                                      Instant now) {
        return next(previous, closure, SyncStatus.PROCESSED, noveltiesApplied, null, now, now);
    }

    /** State after a failed attempt; keeps the last successful processing time and the novelties mark. */
    public static HoursSync pending(HoursSync previous, JournalClosure closure, String error, Instant now) {
        return next(previous, closure, SyncStatus.PENDING, previous != null && previous.noveltiesApplied(),
                abbreviate(error), previous == null ? null : previous.processedAt(), now);
    }

    // Journal data already known is kept when the new attempt does not bring it (processing by hand)
    private static HoursSync next(HoursSync previous, JournalClosure closure, SyncStatus status,
                                  boolean noveltiesApplied, String error, Instant processedAt, Instant now) {
        Long journalId = closure.journalId() != null || previous == null ? closure.journalId() : previous.journalId();
        Long consecutive = closure.consecutive() != null || previous == null
                ? closure.consecutive() : previous.consecutive();
        LocalDateTime closedAt = closure.closedAt() != null || previous == null
                ? closure.closedAt() : previous.closedAt();
        int attempts = previous == null ? 1 : previous.attempts() + 1;
        return new HoursSync(closure.workDate(), journalId, consecutive, closedAt, status, noveltiesApplied, attempts,
                error, processedAt, now);
    }

    private static String abbreviate(String error) {
        if (error == null || error.length() <= MAX_ERROR_LENGTH) {
            return error;
        }
        return error.substring(0, MAX_ERROR_LENGTH - 3) + "...";
    }
}
