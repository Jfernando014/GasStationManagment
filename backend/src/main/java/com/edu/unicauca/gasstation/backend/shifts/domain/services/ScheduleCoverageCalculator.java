package com.edu.unicauca.gasstation.backend.shifts.domain.services;

import com.edu.unicauca.gasstation.backend.shifts.domain.models.DailyCoverage;
import com.edu.unicauca.gasstation.backend.shifts.domain.models.ShiftAssignment;
import com.edu.unicauca.gasstation.backend.shifts.domain.models.ShiftCode;
import com.edu.unicauca.gasstation.backend.shifts.domain.models.ShiftPeriod;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Daily coverage rules shared by {@link ShiftScheduleService} and {@link DayStaffQueryService}, so the schedule seen
 * by the administrator and the day staff seen by other modules never disagree. Pure logic, never stored.
 */
final class ScheduleCoverageCalculator {

    static final String TITULAR = "TITULAR";
    static final String APOYO = "APOYO";

    private ScheduleCoverageCalculator() {
    }

    /**
     * Coverage of every day of the range, including days without any row (those are not covered). A day is covered
     * when there is a titular with a DAY code, a titular with a NIGHT code and, if any code of the day requires
     * support, at least one APOYO shift (any code; the exact paired code is not required).
     */
    static List<DailyCoverage> coverage(LocalDate from, LocalDate to, List<ShiftAssignment> assignments) {
        Map<LocalDate, List<ShiftCode>> codesByDate = assignments.stream()
                .collect(Collectors.groupingBy(ShiftAssignment::getWorkDate,
                        Collectors.mapping(ShiftAssignment::getShiftCode, Collectors.toList())));
        List<DailyCoverage> coverage = new ArrayList<>();
        for (LocalDate date = from; !date.isAfter(to); date = date.plusDays(1)) {
            List<ShiftCode> codes = codesByDate.getOrDefault(date, List.of());
            boolean dayCovered = codes.stream().anyMatch(code -> hasRole(code, TITULAR) && code.getPeriod() == ShiftPeriod.DAY);
            boolean nightCovered = codes.stream().anyMatch(code -> hasRole(code, TITULAR) && code.getPeriod() == ShiftPeriod.NIGHT);
            boolean supportNeeded = codes.stream().anyMatch(ShiftCode::isRequiresSupport);
            boolean supportCovered = !supportNeeded || codes.stream().anyMatch(code -> hasRole(code, APOYO));
            coverage.add(new DailyCoverage(date, dayCovered, nightCovered, supportCovered));
        }
        return coverage;
    }

    static boolean hasRole(ShiftCode code, String roleName) {
        return code.getRole() != null && roleName.equals(code.getRole().getName());
    }
}
