package com.edu.unicauca.gasstation.backend.incentives.domain.models;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Processing state of the additive sales of one journal date. {@code processedAt} is the last time the date was
 * stored successfully, so a date that failed again keeps it.
 */
public record JournalSync(LocalDate journalDate, Long journalId, Long consecutive, LocalDateTime closedAt,
                          SyncStatus status, int attempts, String lastError, Instant processedAt,
                          Instant updatedAt) {

    /** Longest error text that is kept. */
    public static final int MAX_ERROR_LENGTH = 500;

    /** State after storing the date; clears the error. */
    public static JournalSync processed(JournalSync previous, JournalClosure closure, Instant now) {
        return next(previous, closure, SyncStatus.PROCESSED, null, now, now);
    }

    /** State after a failed attempt; keeps the last successful processing time. */
    public static JournalSync pending(JournalSync previous, JournalClosure closure, String error, Instant now) {
        return next(previous, closure, SyncStatus.PENDING, abbreviate(error),
                previous == null ? null : previous.processedAt(), now);
    }

    // Journal data already known is kept when the new attempt does not bring it (processing by hand)
    private static JournalSync next(JournalSync previous, JournalClosure closure, SyncStatus status, String error,
                                    Instant processedAt, Instant now) {
        Long journalId = closure.journalId() != null || previous == null ? closure.journalId() : previous.journalId();
        Long consecutive = closure.consecutive() != null || previous == null
                ? closure.consecutive() : previous.consecutive();
        LocalDateTime closedAt = closure.closedAt() != null || previous == null
                ? closure.closedAt() : previous.closedAt();
        int attempts = previous == null ? 1 : previous.attempts() + 1;
        return new JournalSync(closure.journalDate(), journalId, consecutive, closedAt, status, attempts, error,
                processedAt, now);
    }

    private static String abbreviate(String error) {
        if (error == null || error.length() <= MAX_ERROR_LENGTH) {
            return error;
        }
        return error.substring(0, MAX_ERROR_LENGTH - 3) + "...";
    }
}
