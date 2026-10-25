package com.edu.unicauca.gasstation.backend.shifts;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * Staff of one operational day, returned by {@link DayStaffQuery}. The lists are unmodifiable copies, whoever builds
 * the record.
 *
 * @param date             operational day
 * @param members          who works, titulares first then apoyos
 * @param absentees        who was in the grid but does not work (novelties)
 * @param step1Status      state of step 1 of the journal of that day
 * @param noveltiesApplied true only when step 1 is CONFIRMED and novelties were applied
 * @param warnings         coverage warnings of the day
 */
public record DayStaff(
        LocalDate date,
        List<DayStaffMember> members,
        List<DayStaffAbsentee> absentees,
        Step1Status step1Status,
        boolean noveltiesApplied,
        List<DayStaffWarning> warnings) {

    public DayStaff {
        Objects.requireNonNull(date, "date");
        Objects.requireNonNull(step1Status, "step1Status");
        members = List.copyOf(members);
        absentees = List.copyOf(absentees);
        warnings = List.copyOf(warnings);
    }
}
