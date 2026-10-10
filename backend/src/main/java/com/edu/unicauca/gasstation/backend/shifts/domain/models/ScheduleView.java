package com.edu.unicauca.gasstation.backend.shifts.domain.models;

import java.time.LocalDate;
import java.util.List;

/**
 * Schedule of a date range with its daily coverage.
 *
 * @param from     first day of the range
 * @param to       last day of the range (inclusive)
 * @param workers  active workers with at least one shift in the range (after the filters), ordered by name
 * @param coverage coverage of every day of the range, calculated without the filters
 */
public record ScheduleView(LocalDate from, LocalDate to, List<WorkerShifts> workers, List<DailyCoverage> coverage) {
}
