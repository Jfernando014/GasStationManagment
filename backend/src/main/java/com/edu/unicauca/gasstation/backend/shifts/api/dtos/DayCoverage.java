package com.edu.unicauca.gasstation.backend.shifts.api.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;

/**
 * Coverage of one day, calculated over every active worker (never stored).
 *
 * @param date           day
 * @param dayCovered     at least one titular with a DAY code
 * @param nightCovered   at least one titular with a NIGHT code
 * @param supportCovered at least one support shift when a code of the day requires support; true when none does
 * @param covered        the three conditions above
 */
@Schema(description = "Coverage of one day over every active worker")
public record DayCoverage(
        @Schema(description = "Day", example = "2026-11-03")
        LocalDate date,

        @Schema(description = "At least one titular with a DAY code", example = "true")
        boolean dayCovered,

        @Schema(description = "At least one titular with a NIGHT code", example = "true")
        boolean nightCovered,

        @Schema(description = "At least one support (APOYO) shift when a code of the day requires support; "
                + "true when no code of the day requires it", example = "false")
        boolean supportCovered,

        @Schema(description = "True when the day, night and support conditions are all met", example = "false")
        boolean covered) {
}
