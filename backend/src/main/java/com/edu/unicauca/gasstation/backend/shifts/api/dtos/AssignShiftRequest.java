package com.edu.unicauca.gasstation.backend.shifts.api.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.UUID;

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
