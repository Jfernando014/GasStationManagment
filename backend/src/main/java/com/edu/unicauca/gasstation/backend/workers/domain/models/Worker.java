package com.edu.unicauca.gasstation.backend.workers.domain.models;

import java.util.Objects;
import java.util.UUID;
import lombok.Getter;

/**
 * Seller of the station.
 *
 * <p>Rules that belong to the worker itself:
 * <ul>
 *   <li>Name and document are always kept without leading or trailing spaces.</li>
 *   <li>A new worker starts active.</li>
 *   <li>It is never deleted; it is deactivated so its sales, closings and shifts history is kept.</li>
 *   <li>It can be edited even when inactive.</li>
 * </ul>
 * The role is referenced by id: roles belong to the {@code shifts} module. The dispenser and the products are
 * derived from the role and are never stored here.
 */
@Getter
public class Worker {

    /** Null until the worker is saved. */
    private final UUID id;

    private String fullName;

    /** Identity document (cédula). Text, not a number: it may have leading zeros. */
    private String document;

    private UUID roleId;

    private boolean active;

    /**
     * Creates a new worker, always active.
     */
    public Worker(String fullName, String document, UUID roleId) {
        this(null, fullName, document, roleId, true);
    }

    private Worker(UUID id, String fullName, String document, UUID roleId, boolean active) {
        this.id = id;
        this.active = active;
        updateDetails(fullName, document, roleId);
    }

    /**
     * Rebuilds a worker that already exists (for example, read from the database).
     */
    public static Worker restore(UUID id, String fullName, String document, UUID roleId, boolean active) {
        return new Worker(Objects.requireNonNull(id, "id"), fullName, document, roleId, active);
    }

    /** Replaces the editable data, removing leading and trailing spaces. */
    public void updateDetails(String fullName, String document, UUID roleId) {
        this.fullName = Objects.requireNonNull(fullName, "fullName").strip();
        this.document = Objects.requireNonNull(document, "document").strip();
        this.roleId = Objects.requireNonNull(roleId, "roleId");
    }

    /** Marks the worker as active. Does nothing if it already is. */
    public void activate() {
        this.active = true;
    }

    /** Marks the worker as inactive. Does nothing if it already is. */
    public void deactivate() {
        this.active = false;
    }
}
