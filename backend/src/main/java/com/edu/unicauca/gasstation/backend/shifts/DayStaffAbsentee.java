package com.edu.unicauca.gasstation.backend.shifts;

import java.util.UUID;

/**
 * A worker who was in the grid but does not work the day because of a novelty (absence or replacement).
 * A normal DESCANSO is never an absentee.
 *
 * @param workerId identifier of the worker
 * @param fullName full name, as registered
 * @param gridCode code the worker had in the grid
 * @param type     novelty that took the worker out of the day
 * @param reason   reason of the novelty
 */
public record DayStaffAbsentee(
        UUID workerId,
        String fullName,
        String gridCode,
        NoveltyType type,
        String reason) {
}
