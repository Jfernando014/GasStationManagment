package com.edu.unicauca.gasstation.backend.shifts.api.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Requests and responses of the automatic rotation and of the assignment of a code to a range.
 */
public final class RotationDtos {

    private RotationDtos() {
    }

    /**
     * Body of {@code POST /api/shifts/schedule/rotation/preview}.
     *
     * @param month         month to generate: the current one (from tomorrow) or a future one
     * @param phases        phases chosen by the administrator, keyed by worker id; omitted ones are deduced
     * @param replaceManual rewrite the days set by hand too
     */
    @Schema(description = "Month to preview. Nothing is saved.")
    public record RotationPreviewRequest(
            @Schema(description = "Month to generate (yyyy-MM): the current one, from tomorrow, or a future one",
                    type = "string", example = "2026-11")
            @NotNull(message = "El mes es obligatorio")
            YearMonth month,

            @Schema(description = "Phase (0 to 5) chosen for some titulares, keyed by worker id; it wins over the deduced "
                    + "one. Positions 0 and 1 are DIA, 2 and 3 NOCHE, 4 and 5 DESCANSO on the first day to generate.",
                    example = "{\"d3c8c22b-9a86-4678-a326-ac90b26057cb\": 3}")
            Map<UUID, Integer> phases,

            @Schema(description = "Rewrite the days set by hand too; they are listed in the preview", example = "false")
            boolean replaceManual) {
    }

    /**
     * Body of {@code POST /api/shifts/schedule/rotation/confirm}: the same choices as the preview plus its fingerprint.
     *
     * @param gridVersion fingerprint returned by the preview; the confirmation is rejected if the grid changed since
     */
    @Schema(description = "Confirmation of a previewed month. Saves exactly what the preview showed.")
    public record RotationConfirmRequest(
            @Schema(description = "Month to generate (yyyy-MM)", type = "string", example = "2026-11")
            @NotNull(message = "El mes es obligatorio")
            YearMonth month,

            @Schema(description = "Phases chosen in the preview, keyed by worker id")
            Map<UUID, Integer> phases,

            @Schema(description = "Rewrite the days set by hand too", example = "false")
            boolean replaceManual,

            @Schema(description = "gridVersion returned by the preview",
                    example = "9f2c0e4b6a1d3f5e7c9b8a7d6e5f4c3b2a1908f7e6d5c4b3a29180f7e6d5c4b3")
            @NotBlank(message = "La versión de la malla es obligatoria")
            String gridVersion) {
    }

    /**
     * Preview of the rotation of a month: phases, proposed grid with its coverage, kept days, skipped dates, warnings,
     * counts and the fingerprint to send back with the confirmation.
     */
    @Schema(description = "Preview of the rotation of a month. Nothing has been saved.")
    public record RotationPreviewResponse(
            @Schema(type = "string", example = "2026-11") YearMonth month,
            @Schema(description = "First day to generate; null when there are no days left") LocalDate from,
            @Schema(description = "Last day to generate; null when there are no days left") LocalDate to,
            @Schema(description = "Phase of every active titular") List<TitularPhase> phases,
            @Schema(description = "Proposed grid day by day") List<Day> days,
            @Schema(description = "Days set by hand that are kept") List<KeptDay> kept,
            @Schema(description = "Dates left untouched because they have novelties of the day staff")
            List<LocalDate> skippedDates,
            @Schema(description = "Warnings; they never block the confirmation") List<Warning> warnings,
            Counts counts,
            @Schema(description = "Fingerprint to send with the confirmation") String gridVersion) {

        /** Phase of a titular on the first day to generate. */
        @Schema(name = "RotationTitularPhase")
        public record TitularPhase(
                UUID workerId,
                String fullName,
                @Schema(description = "0 and 1 DIA, 2 and 3 NOCHE, 4 and 5 DESCANSO", example = "3") int phase,
                @Schema(description = "DEDUCED, PROVIDED or SUGGESTED", example = "DEDUCED") String source,
                @Schema(description = "True when the administrator must choose the phase before confirming")
                boolean needsChoice,
                @Schema(description = "Recent days of the grid that do not fit the phase", example = "0") int errors) {
        }

        /** One day of the proposed grid. */
        @Schema(name = "RotationDay")
        public record Day(LocalDate date, List<Row> rows, Coverage coverage) {
        }

        /** One worker on one day. */
        @Schema(name = "RotationRow")
        public record Row(
                UUID workerId,
                String fullName,
                @Schema(example = "9-6") String shiftCode,
                @Schema(description = "GENERATED or MANUAL after confirming", example = "GENERATED") String origin,
                @Schema(description = "NEW, REGENERATED, REPLACED_MANUAL, KEPT_MANUAL, SKIPPED_NOVELTY or UNCHANGED",
                        example = "NEW") String status) {
        }

        /** Coverage of one day of the proposed grid. */
        @Schema(name = "RotationCoverage")
        public record Coverage(
                boolean covered,
                boolean dayCovered,
                boolean nightCovered,
                boolean supportCovered,
                @Schema(example = "[\"12:00 a 18:00\"]") List<String> uncoveredHours) {
        }

        /** A day set by hand that is kept. */
        @Schema(name = "RotationKeptDay")
        public record KeptDay(UUID workerId, String fullName, LocalDate date, String currentCode, String proposedCode) {
        }

        /** One warning with a stable code. */
        @Schema(name = "RotationWarning")
        public record Warning(@Schema(example = "TITULAR_COUNT") String code, String message) {
        }

        /** Days generated, kept and left without full coverage. */
        @Schema(name = "RotationCounts")
        public record Counts(int generated, int kept, int uncovered) {
        }
    }

    /**
     * Result of confirming the rotation of a month.
     *
     * @param generated      days written as generated
     * @param kept           days kept: set by hand, or on a date with novelties of the day staff
     * @param uncoveredDates days left without full coverage
     */
    @Schema(description = "Result of confirming the rotation of a month")
    public record RotationResultResponse(
            @Schema(example = "90") int generated,
            @Schema(example = "2") int kept,
            @Schema(example = "[\"2026-11-05\"]") List<LocalDate> uncoveredDates) {
    }

    /**
     * Body of {@code POST /api/shifts/schedule/rotation}.
     *
     * @param workerId  titular worker to schedule
     * @param startDate first day of the rotation; from tomorrow on
     * @param endDate   last day of the rotation (inclusive); between one full cycle and a quarter from {@code startDate}
     * @param phase     position of {@code startDate} in the cycle (0 to 5); null to deduce it from the grid
     */
    @Schema(description = "Range in which to generate the 2x2 rotation of one titular worker")
    public record GenerateRotationRequest(
            @Schema(description = "Titular worker to schedule", example = "d3c8c22b-9a86-4678-a326-ac90b26057cb")
            @NotNull(message = "El vendedor es obligatorio")
            UUID workerId,

            @Schema(description = "First day of the rotation, from tomorrow on", example = "2026-11-01")
            @NotNull(message = "La fecha inicial es obligatoria")
            LocalDate startDate,

            @Schema(description = "Last day of the rotation (inclusive). At least startDate + 5 days (one full cycle) and "
                    + "at most startDate + 3 months - 1 day (a quarter).", example = "2026-11-30")
            @NotNull(message = "La fecha final es obligatoria")
            LocalDate endDate,

            @Schema(description = "Position of startDate in DIA, DIA, NOCHE, NOCHE, DESCANSO, DESCANSO (0 to 5). Omit it "
                    + "to deduce it from the grid of the previous days.", example = "2")
            Integer phase) {
    }

    /**
     * Body of {@code PUT /api/shifts/schedule/assignments/range}.
     *
     * @param leaveSundaysAsRest put DESCANSO on the Sundays of the range instead of the code
     */
    @Schema(description = "Same code for a worker on every day of a range. Every day is saved as manual.")
    public record AssignRangeRequest(
            @Schema(description = "Worker to schedule", example = "d3c8c22b-9a86-4678-a326-ac90b26057cb")
            @NotNull(message = "El vendedor es obligatorio")
            UUID workerId,

            @Schema(description = "First day of the range, from today on", example = "2026-11-01")
            @NotNull(message = "La fecha inicial es obligatoria")
            LocalDate from,

            @Schema(description = "Last day of the range (inclusive); at most 62 days in total", example = "2026-11-30")
            @NotNull(message = "La fecha final es obligatoria")
            LocalDate to,

            @Schema(description = "Active code of the role of the worker; DESCANSO is valid for every role",
                    example = "6-9y5-9")
            @NotBlank(message = "El código de turno es obligatorio")
            String shiftCode,

            @Schema(description = "Optional note for every day, up to 255 characters", example = "Horario de noviembre")
            @Size(max = 255, message = "La nota no puede superar 255 caracteres")
            String note,

            @Schema(description = "Put DESCANSO on the Sundays of the range", example = "true")
            boolean leaveSundaysAsRest) {
    }
}
