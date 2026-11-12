package com.edu.unicauca.gasstation.backend.workers.domain.models;

import java.time.LocalDate;
import java.util.List;

/**
 * Overtime sheet of a worker in a fortnight, worked out when asked from the hours kept per day.
 *
 * @param days             every day of the fortnight in date order
 * @param subtotals        sum of the categories of the days, without the fixed line or the adjustments
 * @param grossHours       sum of the hours kept for the days, with no rest taken off
 * @param adjustmentsNet   hours the adjustments add minus the ones they subtract, per category
 * @param totalToPay       subtotal + fixed overtime line (in HEDO) + adjustments, per category
 * @param affectedDates    days that are missing, pending or kept without novelties
 * @param parameterVersions start of every parameter version used by the days of the fortnight, in order
 */
public record OvertimeSheet(OvertimeWorker worker, Fortnight fortnight, List<OvertimeDay> days,
                            CategoryHours subtotals, int grossHours, FixedHedoLine fixedHedo,
                            List<OvertimeAdjustment> adjustments, CategoryHours adjustmentsNet,
                            CategoryHours totalToPay, List<AffectedDate> affectedDates,
                            List<LocalDate> parameterVersions) {

    /** True when a day has no record or is pending, so the sheet may be missing hours. */
    public boolean incomplete() {
        return affectedDates.stream().anyMatch(date -> date.reason() != IncompleteReason.NOVELTIES_NOT_APPLIED);
    }

    /** True when a day was kept from the grid, without replacements or absences. */
    public boolean noveltiesUnconfirmed() {
        return affectedDates.stream().anyMatch(date -> date.reason() == IncompleteReason.NOVELTIES_NOT_APPLIED);
    }
}
