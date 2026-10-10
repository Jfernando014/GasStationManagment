package com.edu.unicauca.gasstation.backend.workers.exception;

import java.util.UUID;
import lombok.Getter;

/**
 * The role sent for a worker does not exist in the {@code shifts} module. Translated to 400.
 */
@Getter
public class RoleNotFoundException extends RuntimeException {

    /** Role id that was not found, for logging; it is not part of the user message. */
    private final UUID roleId;

    public RoleNotFoundException(UUID roleId) {
        super("El rol indicado no existe");
        this.roleId = roleId;
    }
}
