package com.edu.unicauca.gasstation.backend.shifts.api.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "Schedule of a date range with its daily coverage")
public record ScheduleResponse(
        @Schema(description = "First day of the range", example = "2026-11-01")
        LocalDate from,

        @Schema(description = "Last day of the range (inclusive)", example = "2026-11-30")
        LocalDate to,

        @Schema(description = "Active workers with at least one shift in the range after the filters, ordered by name")
        List<WorkerSchedule> workers,

        @Schema(description = "Coverage of every day of the range, calculated over every active worker without the "
                + "filters")
        List<DayCoverage> coverage) {
}
