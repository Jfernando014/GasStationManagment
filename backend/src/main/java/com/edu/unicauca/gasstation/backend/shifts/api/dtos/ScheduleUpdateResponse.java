package com.edu.unicauca.gasstation.backend.shifts.api.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.List;

/**
 * Result of changing the schedule.
 *
 * @param saved          number of days saved
 * @param uncoveredDates days of the changed range that are left without coverage; a warning, not an error
 */
@Schema(description = "Result of changing the schedule")
public record ScheduleUpdateResponse(
        @Schema(description = "Number of days saved", example = "30")
        int saved,

        @Schema(description = "Days of the changed range left without coverage (warning, not an error)",
                example = "[\"2026-11-05\", \"2026-11-06\"]")
        List<LocalDate> uncoveredDates) {
}
