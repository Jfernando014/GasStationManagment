package com.edu.unicauca.gasstation.backend.shifts.domain.models;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

/**
 * One worker's month: every day with the code of the schedule, the code actually worked once the active novelties of
 * the day staff are applied, and the novelties seen from this worker. The summary describes what was scheduled; the
 * novelties are counted apart.
 *
 * @param workerId worker identifier, as in the {@code workers} module; it is not validated here
 * @param month    month described
 * @param days     one element per day of the month, in ascending order
 * @param summary  totals of the month
 */
public record WorkerMonth(UUID workerId, YearMonth month, List<Day> days, Summary summary) {

    /**
     * One day of the month.
     *
     * @param date       day
     * @param gridCode   code of the schedule; null when the worker has no row that day
     * @param workedCode code actually worked; null when the worker was absent
     * @param hours      hours of the schedule code according to the catalog; 0 without a row
     * @param note       note of the schedule row; null when there is none
     * @param novelties  active novelties of the day that involve the worker
     */
    public record Day(LocalDate date, String gridCode, String workedCode, int hours, String note,
                      List<Novelty> novelties) {
    }

    /**
     * A novelty seen from the worker.
     *
     * @param kind                what happened to this worker
     * @param originalCode        code the worker had in the schedule that day (DESCANSO without a row); for an
     *                            absence, the code the worker was going to work
     * @param finalCode           code the worker worked instead; null when absent
     * @param reason              reason registered with the novelty
     * @param exceptionalCoverage a code of another role was accepted on purpose
     */
    public record Novelty(Kind kind, String originalCode, String finalCode, String reason,
                          boolean exceptionalCoverage) {
    }

    /** What a novelty meant for the worker. */
    public enum Kind {
        /** The worker did not work, with or without somebody covering. */
        ABSENT,
        /** The worker covered somebody else. */
        COVERING,
        /** The worker worked a different code than the one in the schedule. */
        SHIFT_CHANGE,
        /** The worker traded codes with a colleague. */
        SWAP
    }

    /**
     * Totals of the month, calculated over the schedule.
     *
     * @param scheduledHours   sum of the hours of the schedule codes
     * @param scheduledShifts  days with a code other than DESCANSO
     * @param restDays         days with DESCANSO
     * @param shiftsByCode     how many days each working code has; only codes with at least one day
     * @param replacementsMade novelties where the worker covered somebody
     * @param absences         novelties where the worker did not work, with or without a replacement
     */
    public record Summary(int scheduledHours, int scheduledShifts, int restDays, List<CodeCount> shiftsByCode,
                          int replacementsMade, int absences) {
    }

    /** A working code and the number of days it has in the month. */
    public record CodeCount(String code, int count) {
    }
}
