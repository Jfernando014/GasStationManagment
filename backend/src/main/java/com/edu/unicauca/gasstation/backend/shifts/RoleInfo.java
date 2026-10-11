package com.edu.unicauca.gasstation.backend.shifts;

import java.util.UUID;

/**
 * Read-only view of a role (TITULAR or APOYO) for other modules.
 * Lives in the module root so it is part of the public API of {@code shifts}.
 *
 * @param id        role identifier
 * @param name      role name (TITULAR, APOYO)
 * @param dispenser dispenser the role works on (TITULAR = 1, APOYO = 3)
 */
public record RoleInfo(UUID id, String name, int dispenser) {
}
