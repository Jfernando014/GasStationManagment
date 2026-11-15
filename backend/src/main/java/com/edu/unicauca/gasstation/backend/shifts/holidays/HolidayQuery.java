package com.edu.unicauca.gasstation.backend.shifts.holidays;

import java.time.LocalDate;
import java.util.Set;

/**
 * Colombian public holidays, for other modules. Read only: it never stores anything.
 *
 * <p>Dates are the day off actually observed: a holiday moved to Monday is returned on that Monday. Ordinary Sundays
 * are not holidays here; whoever needs them treats Sundays apart. The station does not count the Chiquinquirá
 * holiday.
 *
 * <p>Compatibility: once published, only {@code default} methods may be added; existing methods are never changed.
 */
public interface HolidayQuery {

    /** Longest range accepted by {@link #getHolidays}, in days (inclusive of both ends). */
    int MAX_RANGE_DAYS = 366;

    /**
     * True if the date is a public holiday.
     *
     * @throws NullPointerException if {@code date} is null
     */
    boolean isHoliday(LocalDate date);

    /**
     * Holidays from {@code from} to {@code to}, both included, in ascending order. Empty when there are none.
     *
     * @throws NullPointerException     if {@code from} or {@code to} is null
     * @throws IllegalArgumentException if {@code to} is before {@code from} or the range is longer than
     *                                  {@link #MAX_RANGE_DAYS} days
     */
    Set<LocalDate> getHolidays(LocalDate from, LocalDate to);
}
