package com.edu.unicauca.gasstation.backend.workers.api.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/**
 * Body of {@code POST /api/workers}. The status is not sent: every new worker starts active.
 *
 * @param fullName full name, up to 150 characters
 * @param document identity document (cédula), up to 20 characters, unique among workers
 * @param roleId   role from the {@code shifts} module (TITULAR or APOYO)
 */
@Schema(description = "Data to create a worker. The new worker always starts active.")
public record CreateWorkerRequest(
        @Schema(description = "Full name, up to 150 characters. Leading and trailing spaces are removed.",
                example = "Ana Milena Villamil")
        @NotBlank(message = "El nombre completo es obligatorio")
        @Size(max = 150, message = "El nombre completo no puede superar 150 caracteres")
        String fullName,

        @Schema(description = "Identity document (cédula), up to 20 characters, unique among workers. "
                + "Text, so leading zeros are kept.", example = "0012345678")
        @NotBlank(message = "El documento es obligatorio")
        @Size(max = 20, message = "El documento no puede superar 20 caracteres")
        String document,

        @Schema(description = "Role identifier (TITULAR or APOYO) from the shift catalog",
                example = "7efc5d6e-e6b5-4093-9348-e062a03eb51a")
        @NotNull(message = "El rol es obligatorio")
        UUID roleId) {
}
