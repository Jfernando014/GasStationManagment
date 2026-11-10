package com.edu.unicauca.gasstation.backend.statistics.domain.models;

import java.time.Instant;
import java.util.Objects;

/** Processing state of one closed operational day, including days with no individual closes. */
public record JournalSalesProcessing(
        ClosedJournalSnapshot journal,
        Status status,
        int attempts,
        Integer saleCount,
        String lastError,
        Instant processedAt,
        Instant updatedAt) {

    public JournalSalesProcessing {
        Objects.requireNonNull(journal, "journal");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(updatedAt, "updatedAt");
        if (attempts < 0) {
            throw new IllegalArgumentException("attempts must not be negative");
        }
        if (status == Status.PROCESSED) {
            if (attempts == 0 || saleCount == null || saleCount < 0 || processedAt == null || lastError != null) {
                throw new IllegalArgumentException("processed state requires a successful attempt and sale count without errors");
            }
        } else if (saleCount != null || processedAt != null) {
            throw new IllegalArgumentException("pending state cannot claim stored sales or successful processing");
        }
    }

    public enum Status {
        PENDING,
        PROCESSED
    }
}
