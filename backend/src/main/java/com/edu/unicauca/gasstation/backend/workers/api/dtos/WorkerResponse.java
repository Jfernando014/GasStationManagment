package com.edu.unicauca.gasstation.backend.workers.api.dtos;

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
public record WorkerResponse(
        UUID id,
        String fullName,
        String document,
        UUID roleId,
        String roleName,
        int dispenser,
        boolean active) {
}
