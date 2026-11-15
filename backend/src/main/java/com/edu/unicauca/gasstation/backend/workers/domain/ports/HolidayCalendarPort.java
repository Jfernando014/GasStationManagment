package com.edu.unicauca.gasstation.backend.workers.domain.ports;

import java.time.LocalDate;
import java.util.Set;

/** Public holidays, read from the shifts module. Sundays are not holidays here. */
public interface HolidayCalendarPort {

    /** Holidays from {@code from} to {@code to}, both included; empty when there are none. */
    Set<LocalDate> holidaysBetween(LocalDate from, LocalDate to);
}
