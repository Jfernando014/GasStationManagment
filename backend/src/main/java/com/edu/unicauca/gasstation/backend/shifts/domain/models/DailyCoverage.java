package com.edu.unicauca.gasstation.backend.shifts.domain.models;

import java.time.LocalDate;

/**
 * Coverage of one day, calculated over every active worker (never stored).
 *
 * @param date           day
 * @param dayCovered     at least one titular with a DAY code
 * @param nightCovered   at least one titular with a NIGHT code
 * @param supportCovered at least one support shift when a code of the day requires support; true when none does
 */
public record DailyCoverage(LocalDate date, boolean dayCovered, boolean nightCovered, boolean supportCovered) {

    /** True when the day, night and support conditions are all met. */
    public boolean covered() {
        return dayCovered && nightCovered && supportCovered;
    }
}
