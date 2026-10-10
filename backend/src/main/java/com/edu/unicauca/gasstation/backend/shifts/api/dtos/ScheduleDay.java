package com.edu.unicauca.gasstation.backend.shifts.api.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;

/**
 * Shift of a worker on one day.
 *
 * @param date      day
 * @param shiftCode code of the catalog
 * @param note      reason for the change; null when there is none
 */
@Schema(description = "Shift of a worker on one day")
public record ScheduleDay(
        @Schema(description = "Day", example = "2026-11-03")
        LocalDate date,

        @Schema(description = "Shift code of the catalog", example = "NOCHE")
        String shiftCode,

        @Schema(description = "Reason for the change; null when there is none", example = "Cambio solicitado por el vendedor")
        String note) {
}
