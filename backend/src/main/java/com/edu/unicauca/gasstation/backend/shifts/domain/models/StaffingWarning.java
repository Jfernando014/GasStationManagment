package com.edu.unicauca.gasstation.backend.shifts.domain.models;

/**
 * Warning of step 1. It never blocks the confirmation (RN-D6).
 *
 * @param type    stable identifier of the warning
 * @param message text in Spanish for the administrator
 */
public record StaffingWarning(Type type, String message) {

    public enum Type {
        /** The day has no grid rows (CB-D1). */
        NO_SCHEDULE,
        /** No titular with a DAY code (VAL-D8). */
        DAY_TITULAR_MISSING,
        /** No titular with a NIGHT code (VAL-D8). */
        NIGHT_TITULAR_MISSING,
        /** A code that requires support (DIA6, 9-6) works without its paired support code (VAL-D9). */
        PAIRING_INCOMPLETE,
        /** A range of hours from 06:00 to 06:00 without anybody (VAL-D10). */
        UNCOVERED_HOURS
    }
}
