package com.edu.unicauca.gasstation.backend.workers.api.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/**
 * Body of {@code PUT /api/workers/{id}}. Does not include the status: activating or deactivating
 * is done with {@code PATCH /api/workers/{id}/status}.
 *
 * @param fullName full name, up to 150 characters
 * @param document identity document (cédula), up to 20 characters, unique among workers
 * @param roleId   role from the {@code shifts} module (TITULAR or APOYO)
 */
@Schema(description = "New data of a worker. The status is changed with PATCH /api/workers/{id}/status.")
public record UpdateWorkerRequest(
        @Schema(description = "Full name, up to 150 characters. Leading and trailing spaces are removed.",
                example = "Ana Milena Villamil Ruiz")
        @NotBlank(message = "El nombre completo es obligatorio")
        @Size(max = 150, message = "El nombre completo no puede superar 150 caracteres")
        String fullName,

        @Schema(description = "Identity document (cédula), up to 20 characters. It must not belong to "
                + "another worker.", example = "0012345678")
        @NotBlank(message = "El documento es obligatorio")
        @Size(max = 20, message = "El documento no puede superar 20 caracteres")
        String document,

        @Schema(description = "Role identifier (TITULAR or APOYO) from the shift catalog",
                example = "2bb7696c-c19e-4b0b-b8ac-bc2cd46f8e7e")
        @NotNull(message = "El rol es obligatorio")
        UUID roleId) {
}
