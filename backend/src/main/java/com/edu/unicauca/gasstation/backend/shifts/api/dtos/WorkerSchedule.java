package com.edu.unicauca.gasstation.backend.shifts.api.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.UUID;

/**
 * Row of the schedule: the days of one worker in the requested range.
 *
 * @param workerId worker identifier
 * @param fullName full name of the worker
 * @param days     scheduled days in ascending order; days without a row are not scheduled
 */
@Schema(description = "Scheduled days of one worker")
public record WorkerSchedule(
        @Schema(description = "Worker identifier", example = "d3c8c22b-9a86-4678-a326-ac90b26057cb")
        UUID workerId,

        @Schema(description = "Full name of the worker", example = "Ana Milena Villamil")
        String fullName,

        @Schema(description = "Scheduled days in ascending order; a missing day is not scheduled")
        List<ScheduleDay> days) {
}
