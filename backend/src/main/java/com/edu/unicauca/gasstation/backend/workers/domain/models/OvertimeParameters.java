package com.edu.unicauca.gasstation.backend.workers.domain.models;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;

/**
 * One version of the overtime parameters, in force from {@code validFrom} until the next version starts. It is never
 * edited: a change is a new version.
 *
 * @param nightStartHour     hour the night starts (the night ends at 06:00)
 * @param restHours          unpaid rest hours taken from a long shift
 * @param restMinShiftHours  hours a shift needs for the rest to be taken
 * @param fixedHedoHours     fixed overtime hours of a fortnight, for the roles that have the switch on
 * @param fixedHedoRoles     names of the roles with the fixed overtime switch on; the others have it off
 * @param reason             why the version was created; null when none was given
 */
public record OvertimeParameters(Long id, LocalDate validFrom, int nightStartHour, int restHours,
                                 int restMinShiftHours, int fixedHedoHours, Set<String> fixedHedoRoles,
                                 String reason, Instant createdAt, String createdBy) {

    /** First hour of the morning, when the night ends. */
    public static final int NIGHT_END_HOUR = 6;

    public static final int MIN_NIGHT_START_HOUR = 12;
    public static final int MAX_NIGHT_START_HOUR = 23;
    public static final int MAX_REST_HOURS = 4;
    public static final int MIN_REST_MIN_SHIFT_HOURS = 1;
    public static final int MAX_REST_MIN_SHIFT_HOURS = 24;
    public static final int MAX_FIXED_HEDO_HOURS = 80;
    public static final int MIN_REASON_LENGTH = 3;
    public static final int MAX_REASON_LENGTH = 255;

    public OvertimeParameters {
        fixedHedoRoles = Set.copyOf(fixedHedoRoles);
    }

    /** True when the fixed overtime line is on for the role. */
    public boolean fixedHedoEnabledFor(String roleName) {
        return roleName != null && fixedHedoRoles.contains(roleName);
    }
}
