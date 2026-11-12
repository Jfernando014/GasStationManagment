package com.edu.unicauca.gasstation.backend.workers.domain.models;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** A journal that sales closed. Everything but the date is null when the date is processed by hand. */
public record JournalClosure(LocalDate workDate, Long journalId, Long consecutive, LocalDateTime closedAt) {

    /** A date to process without a closing announcement. */
    public static JournalClosure ofDate(LocalDate workDate) {
        return new JournalClosure(workDate, null, null, null);
    }
}
