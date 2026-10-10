package com.edu.unicauca.gasstation.backend.shifts.domain.models;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Role a worker plays in a shift (TITULAR or APOYO).
 * The dispenser is derived from the role, not stored on the worker.
 */
@Getter
@AllArgsConstructor
public class Role {

    /** Null until the role is saved. */
    private final UUID id;

    private final String name;

    /** Dispenser the role works on (TITULAR = 1, APOYO = 3). */
    private final int dispenser;
}
