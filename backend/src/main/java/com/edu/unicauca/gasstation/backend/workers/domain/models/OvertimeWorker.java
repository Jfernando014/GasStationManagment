package com.edu.unicauca.gasstation.backend.workers.domain.models;

import java.util.UUID;

/** The worker of a sheet; {@code roleName} is null when the shifts module does not know the role. */
public record OvertimeWorker(UUID id, String fullName, String roleName) {
}
