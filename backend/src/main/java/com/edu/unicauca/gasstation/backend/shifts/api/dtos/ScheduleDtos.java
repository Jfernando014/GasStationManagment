package com.edu.unicauca.gasstation.backend.shifts.api.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Requests and responses of the monthly shift schedule: one day, the schedule of a range with its coverage, and the working hours of a shift code.
 */
public final class ScheduleDtos {

    private ScheduleDtos() {
    }

    /**
     * Body of {@code PUT /api/shifts/schedule/assignments}.
     *
     * @param workerId  worker to schedule
     * @param date      day to schedule; not before today
     * @param shiftCode code of the catalog (DIA, DIA6, 12-7, DESCANSO...)
     * @param note      optional reason for the change, up to 255 characters
     */
    @Schema(description = "Shift code to save for a worker on a day. Replaces the one the day already had.")
    public record AssignShiftRequest(
            @Schema(description = "Worker to schedule", example = "d3c8c22b-9a86-4678-a326-ac90b26057cb")
            @NotNull(message = "El vendedor es obligatorio")
            UUID workerId,

            @Schema(description = "Day to schedule, from today on", example = "2026-11-03")
            @NotNull(message = "La fecha es obligatoria")
            LocalDate date,

            @Schema(description = "Active code of the catalog. Its role must match the worker's role; DESCANSO is valid "
                    + "for every role.", example = "DIA6")
            @NotNull(message = "El código de turno es obligatorio")
            @NotBlank(message = "El código de turno es obligatorio")
            String shiftCode,

            @Schema(description = "Optional reason for the change, up to 255 characters. Leading and trailing spaces are removed; a blank note is stored as null.",
                    example = "Cambio solicitado por el vendedor")
            @Size(max = 255, message = "La nota no puede superar 255 caracteres")
            String note) {
    }

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
}
