package com.edu.unicauca.gasstation.backend.shifts.domain.models;

import java.time.LocalDate;
import java.util.List;

/**
 * Result of changing the schedule (a rotation or a single day).
 *
 * @param saved          number of days saved
 * @param uncoveredDates days of the changed range left without coverage; a warning, not an error
 */
public record ScheduleChange(int saved, List<LocalDate> uncoveredDates) {
}
