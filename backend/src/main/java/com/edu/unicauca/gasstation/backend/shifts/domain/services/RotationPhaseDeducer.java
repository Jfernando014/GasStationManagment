package com.edu.unicauca.gasstation.backend.shifts.domain.services;

import com.edu.unicauca.gasstation.backend.shifts.domain.models.ShiftAssignment;
import com.edu.unicauca.gasstation.backend.shifts.domain.models.ShiftCode;
import com.edu.unicauca.gasstation.backend.shifts.domain.models.ShiftPeriod;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Deduces where a titular is in the 2x2 cycle from the grid of the days before the first day to generate. Pure
 * calculation, without database access.
 *
 * <p>The phase is the position, in DIA, DIA, NOCHE, NOCHE, DESCANSO, DESCANSO, of the first day to generate. The grid
 * code is used, never the code actually worked, so the replacements and absences of the day staff do not change the
 * cycle. Days without a row, days with a note (permissions, sick leave and other changes with a reason) and support
 * codes given to a titular are ignored, so a recent incident does not force the administrator to choose the phase.
 */
final class RotationPhaseDeducer {

    static final int CYCLE = 6;

    private static final int WINDOW_DAYS = 6;
    private static final int WIDE_WINDOW_DAYS = 12;
    private static final int MIN_VALID_DAYS = 2;

    private RotationPhaseDeducer() {
    }

    /**
     * @param phase   deduced phase, or the most likely one when it cannot be deduced (0 without any valid day)
     * @param deduced true when exactly one phase fits every valid day
     * @param errors  valid days that do not fit {@code phase}
     */
    record Deduction(int phase, boolean deduced, int errors) {
    }

    /** Kind of day of a position of the cycle or of a grid code. */
    enum DayKind { DAY, NIGHT, REST }

    /**
     * @param firstDay first day to generate
     * @param rows     grid rows of the titular by date; the days before {@code firstDay} are read
     */
    static Deduction deduce(LocalDate firstDay, Map<LocalDate, ShiftAssignment> rows) {
        List<ValidDay> valid = validDays(firstDay, rows, WINDOW_DAYS);
        if (valid.size() < MIN_VALID_DAYS) {
            valid = validDays(firstDay, rows, WIDE_WINDOW_DAYS);
        }
        if (valid.size() < MIN_VALID_DAYS) {
            return new Deduction(0, false, 0);
        }
        int[] errors = new int[CYCLE];
        for (int phase = 0; phase < CYCLE; phase++) {
            for (ValidDay day : valid) {
                if (kindOf(Math.floorMod(phase - day.daysBefore(), CYCLE)) != day.kind()) {
                    errors[phase]++;
                }
            }
        }
        List<Integer> exact = new ArrayList<>();
        for (int phase = 0; phase < CYCLE; phase++) {
            if (errors[phase] == 0) {
                exact.add(phase);
            }
        }
        if (exact.size() == 1) {
            return new Deduction(exact.get(0), true, 0);
        }
        // Most likely: fewest errors; on a tie, the one that fits the most recent valid day; then the lowest number
        ValidDay mostRecent = valid.get(0);
        int best = 0;
        for (int phase = 1; phase < CYCLE; phase++) {
            if (errors[phase] < errors[best]
                    || (errors[phase] == errors[best] && fits(phase, mostRecent) && !fits(best, mostRecent))) {
                best = phase;
            }
        }
        return new Deduction(best, false, errors[best]);
    }

    /** Kind of day of a position of the cycle: 0 and 1 day, 2 and 3 night, 4 and 5 rest. */
    static DayKind kindOf(int position) {
        return position < 2 ? DayKind.DAY : position < 4 ? DayKind.NIGHT : DayKind.REST;
    }

    private static boolean fits(int phase, ValidDay day) {
        return kindOf(Math.floorMod(phase - day.daysBefore(), CYCLE)) == day.kind();
    }

    /** Valid days of the window, the most recent first. */
    private static List<ValidDay> validDays(LocalDate firstDay, Map<LocalDate, ShiftAssignment> rows, int window) {
        List<ValidDay> valid = new ArrayList<>();
        for (int daysBefore = 1; daysBefore <= window; daysBefore++) {
            ShiftAssignment row = rows.get(firstDay.minusDays(daysBefore));
            if (row == null || row.getNote() != null) {
                continue;
            }
            DayKind kind = kindOf(row.getShiftCode());
            if (kind != null) {
                valid.add(new ValidDay(daysBefore, kind));
            }
        }
        return valid;
    }

    /** Kind of a grid code of a titular; null for a support code, which says nothing about the titular cycle. */
    private static DayKind kindOf(ShiftCode code) {
        if (code.getRole() == null) {
            return DayKind.REST;
        }
        if (!ScheduleCoverageCalculator.hasRole(code, ScheduleCoverageCalculator.TITULAR)) {
            return null;
        }
        return code.getPeriod() == ShiftPeriod.NIGHT ? DayKind.NIGHT : DayKind.DAY;
    }

    private record ValidDay(int daysBefore, DayKind kind) {
    }
}
