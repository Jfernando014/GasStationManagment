package com.edu.unicauca.gasstation.backend.workers.api.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/**
 * Body of {@code PUT /api/workers/{id}}. Does not include the status: activating or deactivating
 * is done with {@code PATCH /api/workers/{id}/status}.
 *
 * @param fullName full name, up to 150 characters (letters, spaces, hyphens or apostrophes); stored with the
 *                 first letter of each word in upper case
 * @param document identity document (cédula), only digits, from 6 to 12, unique among workers
 * @param roleId   role from the {@code shifts} module (TITULAR or APOYO)
 */
@Schema(description = "New data of a worker. The status is changed with PATCH /api/workers/{id}/status.")
public record UpdateWorkerRequest(
        @Schema(description = "Full name, up to 150 characters. It is stored normalized: single spaces and the "
                + "first letter of each word in upper case (connectors such as 'de' or 'del' in lower case).",
                example = "Ana Milena Villamil Ruiz")
        @NotBlank(message = "El nombre completo es obligatorio")
        @Size(max = 150, message = "El nombre completo no puede superar 150 caracteres")
        @Pattern(regexp = "[\\p{L}\\s'\\-]*",
                message = "El nombre solo puede tener letras, espacios, guiones o apóstrofos")
        String fullName,

        @Schema(description = "Identity document (cédula), only digits, from 6 to 12. It must not belong to "
                + "another worker.", example = "0012345678")
        @NotBlank(message = "El documento es obligatorio")
        @Size(min = 6, max = 12, message = "El documento debe tener entre 6 y 12 caracteres")
        @Pattern(regexp = "\\d*", message = "El documento solo puede tener números, sin puntos, guiones ni espacios")
        String document,

        @Schema(description = "Role identifier (TITULAR or APOYO) from the shift catalog",
                example = "2bb7696c-c19e-4b0b-b8ac-bc2cd46f8e7e")
        @NotNull(message = "El rol es obligatorio")
        UUID roleId) {
}
