package com.edu.unicauca.gasstation.backend.workers.api.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

/**
 * Worker as returned by the API.
 *
 * @param id        worker identifier
 * @param fullName  full name
 * @param document  identity document (cédula)
 * @param roleId    role identifier
 * @param roleName  role name (TITULAR, APOYO), read from the {@code shifts} module
 * @param dispenser dispenser derived from the role (TITULAR = 1, APOYO = 3), read from the {@code shifts} module
 * @param active    whether the worker is active
 */
@Schema(description = "Worker with its role")
public record WorkerResponse(
        @Schema(description = "Worker identifier", example = "d3c8c22b-9a86-4678-a326-ac90b26057cb")
        UUID id,

        @Schema(description = "Full name", example = "Ana Milena Villamil")
        String fullName,

        @Schema(description = "Identity document (cédula)", example = "0012345678")
        String document,

        @Schema(description = "Role identifier", example = "7efc5d6e-e6b5-4093-9348-e062a03eb51a")
        UUID roleId,

        @Schema(description = "Role name: TITULAR or APOYO", example = "TITULAR")
        String roleName,

        @Schema(description = "Dispenser derived from the role: TITULAR = 1, APOYO = 3", example = "1")
        int dispenser,

        @Schema(description = "Whether the worker is active. Inactive workers are kept but not assigned to shifts.",
                example = "true")
        boolean active) {
}
