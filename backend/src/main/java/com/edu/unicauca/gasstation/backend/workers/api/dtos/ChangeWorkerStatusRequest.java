package com.edu.unicauca.gasstation.backend.workers.api.dtos;

import jakarta.validation.constraints.NotNull;

/**
 * Body of {@code PATCH /api/workers/{id}/status}.
 *
 * @param active {@code true} to activate the worker, {@code false} to deactivate it
 */
public record ChangeWorkerStatusRequest(
        @NotNull(message = "El estado es obligatorio")
        Boolean active) {
}
