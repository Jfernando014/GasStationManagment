package com.edu.unicauca.gasstation.backend.sales;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Public domain event published by the {@code sales} module when an operational journal is closed (Step 6, C-08).
 *
 * <p>Consumed by:
 * <ul>
 *   <li>{@code sales} (Dev 3): automatic daily savings contribution ({@code S3-08})</li>
 *   <li>{@code workers} (Dev 4): effective worked hours per closed day ({@code S3-13})</li>
 *   <li>{@code incentives} (Dev 4): additive units sold per vendor ({@code S3-16})</li>
 *   <li>{@code inventory} (Dev 5): autonomy alert reconciliation ({@code S3-19})</li>
 *   <li>{@code statistics} (Dev 6): historical shift sales tracking ({@code S3-23})</li>
 * </ul>
 *
 * @param journalId   database identifier of the closed journal
 * @param journalDate operational date of the closed journal
 * @param consecutive sequential planilla number assigned to the journal
 * @param closedAt    timestamp when Step 6 was completed and the journal was locked
 */
public record JournalClosedEvent(
        Long journalId,
        LocalDate journalDate,
        Long consecutive,
        LocalDateTime closedAt
) {

    public JournalClosedEvent {
        Objects.requireNonNull(journalDate, "journalDate must not be null");
        Objects.requireNonNull(consecutive, "consecutive must not be null");
        if (closedAt == null) {
            closedAt = LocalDateTime.now();
        }
    }

    /**
     * Convenience constructor with the operational date and sequential planilla number.
     */
    public JournalClosedEvent(LocalDate journalDate, Long consecutive) {
        this(null, journalDate, consecutive, LocalDateTime.now());
    }

    /**
     * Alias for {@link #journalDate()} for consumers referencing {@code date()}.
     */
    public LocalDate date() {
        return journalDate;
    }

    /**
     * Alias for {@link #consecutive()} for consumers referencing {@code planillaNumber()}.
     */
    public Long planillaNumber() {
        return consecutive;
    }
}
