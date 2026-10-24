package com.edu.unicauca.gasstation.backend.shifts;

/**
 * State of step 1 (day staff) of the journal of a date.
 *
 * <p>UNKNOWN was returned only while this module could not ask the sales module for the journal; now it is
 * never returned.
 */
public enum Step1Status {
    UNKNOWN,
    NO_JOURNAL,
    PENDING,
    CONFIRMED
}
