package com.edu.unicauca.gasstation.backend.workers.api.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/**
 * Body of {@code PATCH /api/workers/{id}/status}.
 *
 * @param active {@code true} to activate the worker, {@code false} to deactivate it
 */
@Schema(description = "New status of a worker")
public record ChangeWorkerStatusRequest(
        @Schema(description = "true to activate the worker, false to deactivate it", example = "false")
        @NotNull(message = "El estado es obligatorio")
        Boolean active) {
}
