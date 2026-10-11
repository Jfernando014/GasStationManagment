package com.edu.unicauca.gasstation.backend.shifts.api.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

/**
 * Role of a shift code as returned by the API.
 *
 * @param id        role identifier
 * @param name      TITULAR or APOYO
 * @param dispenser dispenser the role works on
 */
@Schema(description = "Role of a shift code")
public record RoleResponse(
        @Schema(description = "Role identifier", example = "7efc5d6e-e6b5-4093-9348-e062a03eb51a")
        UUID id,

        @Schema(description = "Role name: TITULAR or APOYO", example = "TITULAR")
        String name,

        @Schema(description = "Dispenser the role works on: TITULAR = 1, APOYO = 3", example = "1")
        int dispenser) {
}
