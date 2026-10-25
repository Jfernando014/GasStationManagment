package com.edu.unicauca.gasstation.backend.shifts.daystaff;

import java.time.LocalDate;
import java.util.List;

/**
 * Who works an operational day, with which code, in which role and on which dispenser.
 * Only entry point other modules may use to read the day staff; it never exposes internal classes.
 *
 * <p>The date is the operational date (the journal runs from 06:00 to 06:00): a NOCHE assignment of day D belongs
 * to D. Past and future dates are accepted. Read only: it never creates or changes the schedule.
 *
 * <p>Compatibility: once published, only enum values and {@code default} methods may be added; record components
 * are never removed or reordered.
 */
public interface DayStaffQuery {

    /** Longest range accepted by the range variant, in days (inclusive of both ends). */
    int MAX_RANGE_DAYS = 62;

    /**
     * Staff of one operational day. Never null; an empty day comes back with a NO_SCHEDULE warning.
     *
     * @throws NullPointerException if {@code date} is null
     */
    DayStaff getDayStaff(LocalDate date);

    /**
     * One DayStaff per day of the range, in ascending date order, including days without schedule.
     *
     * @throws NullPointerException     if {@code from} or {@code to} is null
     * @throws IllegalArgumentException if {@code to} is before {@code from} or the range is longer than
     *                                  {@link #MAX_RANGE_DAYS} days
     */
    List<DayStaff> getDayStaff(LocalDate from, LocalDate to);
}
