package com.edu.unicauca.gasstation.backend.workers.domain.services;

import com.edu.unicauca.gasstation.backend.workers.domain.models.AffectedDate;
import com.edu.unicauca.gasstation.backend.workers.domain.models.CategoryHours;
import com.edu.unicauca.gasstation.backend.workers.domain.models.DayHoursSegment;
import com.edu.unicauca.gasstation.backend.workers.domain.models.Fortnight;
import com.edu.unicauca.gasstation.backend.workers.domain.models.FixedHedoLine;
import com.edu.unicauca.gasstation.backend.workers.domain.models.HoursSync;
import com.edu.unicauca.gasstation.backend.workers.domain.models.IncompleteReason;
import com.edu.unicauca.gasstation.backend.workers.domain.models.OvertimeAdjustment;
import com.edu.unicauca.gasstation.backend.workers.domain.models.OvertimeCategory;
import com.edu.unicauca.gasstation.backend.workers.domain.models.OvertimeDay;
import com.edu.unicauca.gasstation.backend.workers.domain.models.OvertimeParameterHistory;
import com.edu.unicauca.gasstation.backend.workers.domain.models.OvertimeParameters;
import com.edu.unicauca.gasstation.backend.workers.domain.models.OvertimeSheet;
import com.edu.unicauca.gasstation.backend.workers.domain.models.OvertimeWorker;
import com.edu.unicauca.gasstation.backend.workers.domain.models.SyncStatus;
import com.edu.unicauca.gasstation.backend.workers.domain.models.WorkerDayHours;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Works out the fortnight overtime sheet from the hours kept per day. Pure: it reads nothing, so every rule is here.
 *
 * <p>Each hour of a shift is classified with the calendar day it falls on: the hours after midnight of a night
 * shift belong to the next calendar day for the Sunday and holiday rule, but the shift stays on the day it
 * started. A night hour is one from the night start hour to 06:00; a festive day is a Sunday or a public holiday.
 * Daytime hours of an ordinary day earn no surcharge. The unpaid rest is taken off the end of a long shift, and the
 * parameters are those in force on the day the shift starts, whatever day each hour falls on.
 */
public final class OvertimeCalculator {

    private OvertimeCalculator() {
    }

    /**
     * Sheet of one worker.
     *
     * @param recordsByDate the worker's kept hours by operational date; days without entry are rest or unknown
     * @param holidays      public holidays covering the fortnight and the morning after its last day
     */
    public static OvertimeSheet sheet(OvertimeWorker worker, Fortnight fortnight,
                                      Map<LocalDate, WorkerDayHours> recordsByDate, Set<LocalDate> holidays,
                                      OvertimeParameterHistory parameters, List<OvertimeAdjustment> adjustments,
                                      List<AffectedDate> affectedDates) {
        List<OvertimeDay> days = new ArrayList<>();
        Set<LocalDate> versions = new LinkedHashSet<>();
        CategoryHours subtotals = CategoryHours.ZERO;
        int grossHours = 0;
        for (LocalDate date = fortnight.from(); !date.isAfter(fortnight.to()); date = date.plusDays(1)) {
            OvertimeParameters applying = parameters.applyingOn(date);
            OvertimeDay day = day(date, recordsByDate.get(date), holidays, applying);
            days.add(day);
            versions.add(applying.validFrom());
            subtotals = subtotals.plus(day.hours());
            grossHours += day.grossHours();
        }
        // The fixed line belongs to the whole fortnight, so it uses the version in force on its last day
        OvertimeParameters closing = parameters.applyingOn(fortnight.to());
        boolean fixedOn = closing.fixedHedoEnabledFor(worker.roleName());
        FixedHedoLine fixedHedo = new FixedHedoLine(fixedOn, fixedOn ? closing.fixedHedoHours() : 0);
        CategoryHours adjustmentsNet = CategoryHours.ZERO;
        for (OvertimeAdjustment adjustment : adjustments) {
            adjustmentsNet = adjustmentsNet.plus(adjustment.category(), adjustment.signedHours());
        }
        CategoryHours totalToPay = subtotals.plus(OvertimeCategory.HEDO, fixedHedo.hours()).plus(adjustmentsNet);
        return new OvertimeSheet(worker, fortnight, List.copyOf(days), subtotals, grossHours, fixedHedo,
                List.copyOf(adjustments), adjustmentsNet, totalToPay, List.copyOf(affectedDates),
                List.copyOf(versions));
    }

    /** Days of the fortnight that are missing, pending or kept without their novelties, in date order. */
    public static List<AffectedDate> affectedDates(Fortnight fortnight, Map<LocalDate, HoursSync> syncByDate) {
        List<AffectedDate> affected = new ArrayList<>();
        for (LocalDate date = fortnight.from(); !date.isAfter(fortnight.to()); date = date.plusDays(1)) {
            HoursSync sync = syncByDate.get(date);
            if (sync == null) {
                affected.add(new AffectedDate(date, IncompleteReason.MISSING_RECORD));
            } else if (sync.status() == SyncStatus.PENDING) {
                affected.add(new AffectedDate(date, IncompleteReason.PENDING));
            } else if (!sync.noveltiesApplied()) {
                affected.add(new AffectedDate(date, IncompleteReason.NOVELTIES_NOT_APPLIED));
            }
        }
        return affected;
    }

    private static OvertimeDay day(LocalDate date, WorkerDayHours record, Set<LocalDate> holidays,
                                   OvertimeParameters parameters) {
        boolean festive = isFestive(date, holidays);
        if (record == null || record.absent()) {
            return new OvertimeDay(date, date.getDayOfWeek(), festive, null, 0, CategoryHours.ZERO,
                    parameters.validFrom());
        }
        return new OvertimeDay(date, date.getDayOfWeek(), festive, record.workedCode(), record.totalHours(),
                classify(record, holidays, parameters), parameters.validFrom());
    }

    /** Surcharge hours of the shift of a day, after the unpaid rest. */
    private static CategoryHours classify(WorkerDayHours record, Set<LocalDate> holidays,
                                          OvertimeParameters parameters) {
        List<int[]> hours = hoursOfShift(record.segments());
        if (parameters.restHours() > 0 && hours.size() >= parameters.restMinShiftHours()) {
            // The rest comes off the last hours of the shift, the ones that end where the shift ends
            int kept = Math.max(0, hours.size() - parameters.restHours());
            hours = hours.subList(0, kept);
        }
        CategoryHours result = CategoryHours.ZERO;
        for (int[] hour : hours) {
            boolean festive = isFestive(record.workDate().plusDays(hour[0]), holidays);
            boolean night = hour[1] >= parameters.nightStartHour() || hour[1] < OvertimeParameters.NIGHT_END_HOUR;
            if (festive && !night) {
                result = result.plus(OvertimeCategory.RDF, 1);
            } else if (night && !festive) {
                result = result.plus(OvertimeCategory.RNO, 1);
            } else if (night) {
                result = result.plus(OvertimeCategory.RNF, 1);
            }
        }
        return result;
    }

    /** Every worked hour in order, as {days after the shift's day, hour of the day}. */
    private static List<int[]> hoursOfShift(List<DayHoursSegment> segments) {
        List<int[]> hours = new ArrayList<>();
        for (DayHoursSegment segment : segments) {
            if (segment.endHour() > segment.startHour()) {
                for (int hour = segment.startHour(); hour < segment.endHour(); hour++) {
                    hours.add(new int[] {0, hour});
                }
            } else {
                for (int hour = segment.startHour(); hour < 24; hour++) {
                    hours.add(new int[] {0, hour});
                }
                for (int hour = 0; hour < segment.endHour(); hour++) {
                    hours.add(new int[] {1, hour});
                }
            }
        }
        return hours;
    }

    private static boolean isFestive(LocalDate date, Set<LocalDate> holidays) {
        return date.getDayOfWeek() == DayOfWeek.SUNDAY || holidays.contains(date);
    }
}
