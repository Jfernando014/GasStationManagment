package com.edu.unicauca.gasstation.backend.workers.api.dtos;

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
public record UpdateWorkerRequest(
        @NotBlank(message = "El nombre completo es obligatorio")
        @Size(max = 150, message = "El nombre completo no puede superar 150 caracteres")
        String fullName,

        @NotBlank(message = "El documento es obligatorio")
        @Size(max = 20, message = "El documento no puede superar 20 caracteres")
        String document,

        @NotNull(message = "El rol es obligatorio")
        UUID roleId) {
}
