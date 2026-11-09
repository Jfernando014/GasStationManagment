package com.edu.unicauca.gasstation.backend.shifts.api.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.YearMonth;
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
     * Month of one worker: every day with the schedule code, the code worked and the novelties of the day staff, plus
     * the totals of the month.
     *
     * @param workerId worker identifier
     * @param month    month described
     * @param days     one element per day of the month
     * @param summary  totals of the month, calculated over the schedule
     */
    @Schema(description = "Mes de un vendedor: cada día con el código de la malla, el código trabajado y sus "
            + "novedades, más los totales del mes")
    public record WorkerMonthResponse(
            @Schema(description = "Identificador del vendedor", example = "d3c8c22b-9a86-4678-a326-ac90b26057cb")
            UUID workerId,

            @Schema(description = "Mes consultado (yyyy-MM)", type = "string", example = "2026-08")
            YearMonth month,

            @Schema(description = "Un elemento por cada día del mes, en orden")
            List<WorkerMonthDay> days,

            @Schema(description = "Totales del mes; se calculan sobre la malla, es decir, lo programado")
            WorkerMonthSummary summary) {
    }

    /**
     * One day of the month of a worker.
     *
     * @param date       day
     * @param gridCode   code of the schedule; null when the worker has no row
     * @param workedCode code actually worked once the novelties apply; null when the worker was absent
     * @param hours      hours of the schedule code according to the catalog; 0 without a row
     * @param note       note of the schedule row; null when there is none
     * @param novelties  active novelties of the day that involve the worker
     */
    @Schema(description = "Un día del mes de un vendedor")
    public record WorkerMonthDay(
            @Schema(description = "Día", example = "2026-08-12")
            LocalDate date,

            @Schema(description = "Código de la malla; nulo si el vendedor no tiene fila ese día", example = "DIA6",
                    nullable = true)
            String gridCode,

            @Schema(description = "Código realmente trabajado con las novedades activas; nulo si estuvo ausente",
                    example = "NOCHE", nullable = true)
            String workedCode,

            @Schema(description = "Horas del código de la malla según el catálogo; 0 si no hay fila", example = "6")
            int hours,

            @Schema(description = "Nota de la malla; nula si no hay", example = "Cambio solicitado por el vendedor",
                    nullable = true)
            String note,

            @Schema(description = "Novedades activas de ese día vistas desde este vendedor")
            List<WorkerMonthNovelty> novelties) {
    }

    /**
     * A novelty seen from the worker.
     *
     * @param kind                ABSENT, COVERING, SHIFT_CHANGE or SWAP
     * @param originalCode        code the worker had in the schedule that day; DESCANSO without a row
     * @param finalCode           code the worker worked instead; null when absent
     * @param reason              reason registered with the novelty
     * @param exceptionalCoverage a code of another role was accepted on purpose
     */
    @Schema(description = "Novedad de un día vista desde el vendedor")
    public record WorkerMonthNovelty(
            @Schema(description = "Qué le pasó al vendedor: ABSENT = faltó (con o sin reemplazo), COVERING = cubrió a "
                    + "otro, SHIFT_CHANGE = trabajó otro código, SWAP = intercambió turno con un compañero",
                    example = "SWAP", allowableValues = {"ABSENT", "COVERING", "SHIFT_CHANGE", "SWAP"})
            String kind,

            @Schema(description = "Código que el vendedor tenía en la malla ese día (DESCANSO si no tenía fila); en una "
                    + "ausencia, el código que iba a trabajar", example = "DIA")
            String originalCode,

            @Schema(description = "Código que trabajó en su lugar; nulo si estuvo ausente", example = "NOCHE",
                    nullable = true)
            String finalCode,

            @Schema(description = "Motivo registrado con la novedad", example = "Cita médica")
            String reason,

            @Schema(description = "Se aceptó a propósito un código de otro rol", example = "false")
            boolean exceptionalCoverage) {
    }

    /**
     * Totals of the month of a worker, over the schedule.
     *
     * @param scheduledHours   sum of the hours of the schedule codes
     * @param scheduledShifts  days with a code other than DESCANSO
     * @param restDays         days with DESCANSO
     * @param shiftsByCode     days of each working code; only codes with at least one day
     * @param replacementsMade times the worker covered a colleague
     * @param absences         days the worker did not work, with or without a replacement
     */
    @Schema(description = "Totales del mes de un vendedor, calculados sobre la malla")
    public record WorkerMonthSummary(
            @Schema(description = "Suma de las horas de los códigos de la malla", example = "225")
            int scheduledHours,

            @Schema(description = "Días con un código distinto de DESCANSO", example = "21")
            int scheduledShifts,

            @Schema(description = "Días con DESCANSO", example = "10")
            int restDays,

            @Schema(description = "Cantidad de días de cada código de trabajo; solo los que tienen algún día")
            List<WorkerMonthCodeCount> shiftsByCode,

            @Schema(description = "Veces que cubrió a un compañero", example = "0")
            int replacementsMade,

            @Schema(description = "Días que faltó, con o sin reemplazo", example = "0")
            int absences) {
    }

    /**
     * A working code and the number of days it has in the month.
     *
     * @param code  code of the catalog
     * @param count days with that code
     */
    @Schema(description = "Un código de trabajo y cuántos días tiene en el mes")
    public record WorkerMonthCodeCount(
            @Schema(description = "Código del catálogo", example = "NOCHE")
            String code,

            @Schema(description = "Días con ese código", example = "10")
            int count) {
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
