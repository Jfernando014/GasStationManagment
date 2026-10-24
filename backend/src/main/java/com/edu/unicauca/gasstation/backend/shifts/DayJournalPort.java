package com.edu.unicauca.gasstation.backend.shifts;

import java.time.LocalDate;

/**
 * What the shifts module needs to know about the journal of a date (S2-19). Declared here and implemented by the
 * {@code sales} module, which owns the journals: {@code sales} already depends on {@code shifts}, so the opposite
 * dependency would be a cycle (same pattern as {@link ScheduleWorkerDirectory}).
 *
 * <p>Until {@code sales} publishes its implementation, {@code shifts} provides a temporary in-memory one that is
 * used only when no other bean of this type exists.
 */
public interface DayJournalPort {

    /** Step-1 state of the journal of that date: NO_JOURNAL, PENDING or CONFIRMED. */
    Step1Status step1Status(LocalDate date);

    /** True when the journal of that date exists and can still be edited (it is not closed). */
    boolean isEditable(LocalDate date);

    /** Marks step 1 of the journal of that date as completed. Joins the caller's transaction. */
    void markStep1Completed(LocalDate date);
}
