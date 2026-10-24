package com.edu.unicauca.gasstation.backend.shifts;

/**
 * State of step 1 (day staff) of the journal of a date.
 *
 * <p>UNKNOWN is returned only while this module cannot ask the sales module (phase 1 of S2-20); afterwards it is
 * never returned.
 */
public enum Step1Status {
    UNKNOWN,
    NO_JOURNAL,
    PENDING,
    CONFIRMED
}
