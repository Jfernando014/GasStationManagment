package com.edu.unicauca.gasstation.backend.shifts.domain.models;

/**
 * Warning of step 1. It never blocks the confirmation.
 *
 * @param type    stable identifier of the warning
 * @param message text in Spanish for the administrator
 */
public record StaffingWarning(Type type, String message) {

    public enum Type {
        /** The day has no grid rows. */
        NO_SCHEDULE,
        /** No titular with a DAY code. */
        DAY_TITULAR_MISSING,
        /** No titular with a NIGHT code. */
        NIGHT_TITULAR_MISSING,
        /** A code that requires support (DIA6, 9-6) works without its paired support code. */
        PAIRING_INCOMPLETE,
        /** A range of hours from 06:00 to 06:00 without anybody. */
        UNCOVERED_HOURS
    }
}
