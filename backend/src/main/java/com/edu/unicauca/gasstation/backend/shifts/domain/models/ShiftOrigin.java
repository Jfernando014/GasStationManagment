package com.edu.unicauca.gasstation.backend.shifts.domain.models;

/**
 * Who put a day of the grid. The automatic rotation only rewrites empty or generated days, so a day the administrator
 * set by hand is never overwritten without permission.
 */
public enum ShiftOrigin {
    /** Written by the automatic rotation; it never has a note. */
    GENERATED,
    /** Written by the administrator, editing one day or a range. */
    MANUAL
}
