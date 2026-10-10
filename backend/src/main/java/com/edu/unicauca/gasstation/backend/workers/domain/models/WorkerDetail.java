package com.edu.unicauca.gasstation.backend.workers.domain.models;

import com.edu.unicauca.gasstation.backend.shifts.RoleInfo;
import java.util.Objects;

/**
 * A worker together with its role, read from the {@code shifts} module.
 * Returned by the use cases so the API can show the role name and the dispenser without calling {@code shifts}.
 *
 * @param worker the worker
 * @param role   its role; never null
 */
public record WorkerDetail(Worker worker, RoleInfo role) {

    public WorkerDetail {
        Objects.requireNonNull(worker, "worker");
        Objects.requireNonNull(role, "role");
    }
}
