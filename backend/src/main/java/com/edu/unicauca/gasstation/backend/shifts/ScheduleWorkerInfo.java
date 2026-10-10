package com.edu.unicauca.gasstation.backend.shifts;

import java.util.UUID;

/**
 * Data of a worker that the shift schedule needs. Returned by {@link ScheduleWorkerDirectory}.
 *
 * @param id       worker identifier
 * @param fullName full name, shown in the schedule
 * @param roleId   role of the worker (a role of this module)
 * @param active   inactive workers cannot be scheduled and are not shown
 */
public record ScheduleWorkerInfo(UUID id, String fullName, UUID roleId, boolean active) {
}
