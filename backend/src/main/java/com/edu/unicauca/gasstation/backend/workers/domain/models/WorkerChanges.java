package com.edu.unicauca.gasstation.backend.workers.domain.models;

import java.util.Objects;
import java.util.UUID;

/**
 * New values for an existing worker: what the edit use case may change.
 * Name and document are stored without leading or trailing spaces, so the duplicate check compares the
 * document exactly as it will be saved.
 *
 * @param fullName new full name
 * @param document new identity document (cédula)
 * @param roleId   new role
 */
public record WorkerChanges(String fullName, String document, UUID roleId) {

    public WorkerChanges {
        fullName = Objects.requireNonNull(fullName, "fullName").strip();
        document = Objects.requireNonNull(document, "document").strip();
        Objects.requireNonNull(roleId, "roleId");
    }
}
