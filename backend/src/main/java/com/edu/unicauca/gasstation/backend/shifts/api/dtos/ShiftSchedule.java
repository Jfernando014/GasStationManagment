package com.edu.unicauca.gasstation.backend.shifts.api.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * Working segments of a shift. A segment whose endHour is lower than its startHour ends the next day.
 *
 * @param segments   one or two blocks of hours; empty for DESCANSO
 * @param totalHours sum of the hours of every segment
 */
@Schema(description = "Working hours of a shift code")
public record ShiftSchedule(
        @Schema(description = "One or two blocks of working hours; empty for DESCANSO")
        List<Segment> segments,

        @Schema(description = "Sum of the hours of every segment", example = "12")
        int totalHours) {

    /**
     * One block of working hours.
     *
     * @param startHour hour the block starts (0-23)
     * @param endHour   hour the block ends (1-24); lower than startHour means it ends the next day
     */
    @Schema(description = "One block of working hours")
    public record Segment(
            @Schema(description = "Hour the block starts, 0-23", example = "18")
            int startHour,

            @Schema(description = "Hour the block ends, 1-24. Lower than startHour means it ends the next day.",
                    example = "6")
            int endHour) {
    }
}
