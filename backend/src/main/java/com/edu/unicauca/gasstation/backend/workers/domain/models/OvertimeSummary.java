package com.edu.unicauca.gasstation.backend.workers.domain.models;

import java.util.List;

/**
 * Sheets of every worker with hours or adjustments in a fortnight. The days are kept per date, not per worker, so
 * {@code affectedDates} is the same for all of them.
 */
public record OvertimeSummary(Fortnight fortnight, List<OvertimeSheet> sheets, List<AffectedDate> affectedDates) {

    public boolean incomplete() {
        return affectedDates.stream().anyMatch(date -> date.reason() != IncompleteReason.NOVELTIES_NOT_APPLIED);
    }

    public boolean noveltiesUnconfirmed() {
        return affectedDates.stream().anyMatch(date -> date.reason() == IncompleteReason.NOVELTIES_NOT_APPLIED);
    }
}
