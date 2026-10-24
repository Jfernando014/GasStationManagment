package com.edu.unicauca.gasstation.backend.shifts.domain.services;

import static com.edu.unicauca.gasstation.backend.shifts.domain.services.ScheduleCoverageCalculator.TITULAR;

import com.edu.unicauca.gasstation.backend.shifts.ScheduleWorkerInfo;
import com.edu.unicauca.gasstation.backend.shifts.domain.models.DayLineup;
import com.edu.unicauca.gasstation.backend.shifts.domain.models.ShiftCode;
import com.edu.unicauca.gasstation.backend.shifts.domain.models.ShiftPeriod;
import com.edu.unicauca.gasstation.backend.shifts.domain.models.StaffingWarning;
import com.edu.unicauca.gasstation.backend.shifts.domain.models.StaffingWarning.Type;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.IntStream;

/**
 * Warnings of step 1 (VAL-D8 to VAL-D10). They only inform: none of them blocks the confirmation (RN-D6).
 * Calculated over the codes actually worked, after the novelties.
 */
final class StaffingAdvisor {

    /** Hours of an operational day in order: from 06:00 to 06:00 of the next day (CB-D3). */
    private static final List<Integer> OPERATIONAL_HOURS =
            IntStream.range(6, 30).map(hour -> hour % 24).boxed().toList();

    private StaffingAdvisor() {
    }

    /**
     * @param noSchedule true when the day has no grid rows of active workers (CB-D1)
     * @param working    working lines of active workers
     * @param workers    active workers by id, for the names in the messages
     */
    static List<StaffingWarning> warnings(boolean noSchedule, List<DayLineup.Line> working,
                                          Map<UUID, ScheduleWorkerInfo> workers) {
        List<StaffingWarning> warnings = new ArrayList<>();
        if (noSchedule) {
            warnings.add(new StaffingWarning(Type.NO_SCHEDULE, "Sin malla para este día"));
            if (working.isEmpty()) {
                return warnings;
            }
        }
        List<ShiftCode> codes = working.stream().map(DayLineup.Line::workedCode).toList();
        if (codes.stream().noneMatch(code -> isTitular(code, ShiftPeriod.DAY))) {
            warnings.add(new StaffingWarning(Type.DAY_TITULAR_MISSING, "Falta titular de día"));
        }
        if (codes.stream().noneMatch(code -> isTitular(code, ShiftPeriod.NIGHT))) {
            warnings.add(new StaffingWarning(Type.NIGHT_TITULAR_MISSING, "Falta titular de noche"));
        }
        for (DayLineup.Line line : working) {
            ShiftCode code = line.workedCode();
            boolean paired = codes.stream().anyMatch(other -> code.getId().equals(other.getPairedWithId()));
            if (code.isRequiresSupport() && !paired) {
                warnings.add(new StaffingWarning(Type.PAIRING_INCOMPLETE, "Acople incompleto: "
                        + workers.get(line.workerId()).fullName() + " en " + code.getCode()
                        + " sin su apoyo acoplado"));
            }
        }
        uncoveredRanges(codes).forEach(range -> warnings.add(new StaffingWarning(Type.UNCOVERED_HOURS,
                "Sin personal de %02d:00 a %02d:00".formatted(range[0], (range[1] + 1) % 24))));
        return warnings;
    }

    private static boolean isTitular(ShiftCode code, ShiftPeriod period) {
        return ScheduleCoverageCalculator.hasRole(code, TITULAR) && code.getPeriod() == period;
    }

    /** Consecutive uncovered hours of the operational day, as {first hour, last hour}. */
    private static List<int[]> uncoveredRanges(List<ShiftCode> codes) {
        Set<Integer> covered = new HashSet<>();
        codes.forEach(code -> covered.addAll(code.coveredHours()));
        List<int[]> ranges = new ArrayList<>();
        Integer first = null;
        int last = 0;
        for (int hour : OPERATIONAL_HOURS) {
            if (!covered.contains(hour)) {
                first = first == null ? hour : first;
                last = hour;
            } else if (first != null) {
                ranges.add(new int[]{first, last});
                first = null;
            }
        }
        if (first != null) {
            ranges.add(new int[]{first, last});
        }
        return ranges;
    }
}
