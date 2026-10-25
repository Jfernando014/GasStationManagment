package com.edu.unicauca.gasstation.backend.shifts;

/**
 * Coverage warning of a day. NO_SCHEDULE is returned alone, only when the day has no active assignment; the other
 * three follow the same rules as the coverage of the shift schedule.
 */
public enum DayStaffWarning {
    NO_SCHEDULE,
    DAY_TITULAR_MISSING,
    NIGHT_TITULAR_MISSING,
    SUPPORT_MISSING
}
