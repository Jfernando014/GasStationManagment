package com.edu.unicauca.gasstation.backend.shifts.api.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Body of {@code POST /api/shifts/schedule/rotation}.
 *
 * @param workerId  titular worker to schedule
 * @param startDate first day of the rotation; not before today
 * @param endDate   last day of the rotation (inclusive); between one full cycle and a quarter from {@code startDate}
 */
@Schema(description = "Range in which to generate the 2x2 rotation of a titular worker")
public record GenerateRotationRequest(
        @Schema(description = "Titular worker to schedule", example = "d3c8c22b-9a86-4678-a326-ac90b26057cb")
        @NotNull(message = "El vendedor es obligatorio")
        UUID workerId,

        @Schema(description = "First day of the rotation, from today on. The pattern starts on this day.",
                example = "2026-11-01")
        @NotNull(message = "La fecha inicial es obligatoria")
        LocalDate startDate,

        @Schema(description = "Last day of the rotation (inclusive). At least startDate + 5 days (one full cycle) and "
                + "at most startDate + 3 months - 1 day (a quarter).", example = "2026-11-30")
        @NotNull(message = "La fecha final es obligatoria")
        LocalDate endDate) {
}
