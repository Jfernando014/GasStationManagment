package com.edu.unicauca.gasstation.backend.shifts.domain.models;

import java.util.List;
import java.util.UUID;

/**
 * Shifts of one worker in a range of the schedule.
 *
 * @param workerId    worker identifier
 * @param fullName    full name, read from the {@code workers} module
 * @param assignments assignments in ascending date order, with their shift code loaded
 */
public record WorkerShifts(UUID workerId, String fullName, List<ShiftAssignment> assignments) {
}
