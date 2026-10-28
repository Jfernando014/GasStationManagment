package com.edu.unicauca.gasstation.backend.shifts.domain.models;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

/**
 * Result of calculating the automatic rotation of a month without saving anything. The confirmation recalculates the
 * same plan and saves it only when {@code gridVersion} still matches the grid.
 *
 * @param month        month asked for
 * @param from         first day to generate (tomorrow at the earliest); null when there are no days left
 * @param to           last day of the month; null when there are no days left
 * @param phases       phase of every active titular, in name order
 * @param days         proposed grid day by day, with the rows of every active worker and the coverage
 * @param kept         manual days the rotation keeps, with the code it would have written
 * @param skippedDates dates left untouched because they already have novelties of the day staff
 * @param warnings     problems found; they never block the confirmation
 * @param counts       generated, kept and uncovered days
 * @param gridVersion  fingerprint of the grid the plan was calculated from
 */
public record RotationPreview(
        YearMonth month,
        LocalDate from,
        LocalDate to,
        List<TitularPhase> phases,
        List<Day> days,
        List<KeptDay> kept,
        List<LocalDate> skippedDates,
        List<Warning> warnings,
        Counts counts,
        String gridVersion) {

    public RotationPreview {
        phases = List.copyOf(phases);
        days = List.copyOf(days);
        kept = List.copyOf(kept);
        skippedDates = List.copyOf(skippedDates);
        warnings = List.copyOf(warnings);
    }

    /** Where the phase of a titular comes from. */
    public enum PhaseSource {
        /** Exactly one phase fits the recent grid. */
        DEDUCED,
        /** Chosen by the administrator; always wins over the deduced one. */
        PROVIDED,
        /** Could not be deduced: the most likely one, which the administrator must confirm. */
        SUGGESTED
    }

    /** What the rotation does with one titular on one day, or with the row of another worker. */
    public enum RowStatus {
        /** The day had no row: it is created as generated. */
        NEW,
        /** The day was generated: it is written again, still generated. */
        REGENERATED,
        /** The day was manual and the administrator asked to replace manual days: it becomes generated. */
        REPLACED_MANUAL,
        /** The day is manual: it is kept as it is. */
        KEPT_MANUAL,
        /** The date has novelties of the day staff: nothing of that date is written. */
        SKIPPED_NOVELTY,
        /** A row the rotation does not handle (support workers): shown for the coverage, never written. */
        UNCHANGED
    }

    /**
     * Phase of one titular: position in DIA, DIA, NOCHE, NOCHE, DESCANSO, DESCANSO of the first day to generate.
     *
     * @param needsChoice true when the phase could not be deduced and the administrator has not chosen it
     * @param errors      recent days of the grid that do not fit the phase
     */
    public record TitularPhase(UUID workerId, String fullName, int phase, PhaseSource source, boolean needsChoice,
                               int errors) {
    }

    /** One day of the proposed grid. */
    public record Day(LocalDate date, List<Row> rows, Coverage coverage) {

        public Day {
            rows = List.copyOf(rows);
        }
    }

    /**
     * One worker on one day of the proposed grid.
     *
     * @param shiftCode code the day will have after confirming; null when the worker has no row that day
     * @param origin    origin the day will have after confirming; null when the worker has no row that day
     */
    public record Row(UUID workerId, String fullName, String shiftCode, ShiftOrigin origin, RowStatus status) {
    }

    /**
     * Coverage of one day of the proposed grid.
     *
     * @param covered        every hour from 06:00 to 06:00 has somebody, there are a day and a night titular and every
     *                       code that needs support has it
     * @param uncoveredHours ranges of hours without anybody, such as "12:00 a 18:00"
     */
    public record Coverage(boolean covered, boolean dayCovered, boolean nightCovered, boolean supportCovered,
                           List<String> uncoveredHours) {

        public Coverage {
            uncoveredHours = List.copyOf(uncoveredHours);
        }
    }

    /** A manual day the rotation keeps, with the code it would have written there. */
    public record KeptDay(UUID workerId, String fullName, LocalDate date, String currentCode, String proposedCode) {
    }

    /** A problem of the plan, with a stable code and a message in Spanish. It never blocks. */
    public record Warning(String code, String message) {
    }

    /**
     * @param generated rows written as generated (new, regenerated or replacing manual days)
     * @param kept      rows kept: manual days and the rows of dates with novelties
     * @param uncovered days left without full coverage
     */
    public record Counts(int generated, int kept, int uncovered) {
    }

    /** What the confirmation saved. */
    public record Outcome(int generated, int kept, List<LocalDate> uncoveredDates) {

        public Outcome {
            uncoveredDates = List.copyOf(uncoveredDates);
        }
    }
}
