package com.edu.unicauca.gasstation.backend.workers.domain.models;

import java.util.UUID;

/**
 * Optional filters for listing workers. Every field can be null (or blank, for {@code search}) to ignore it.
 *
 * @param active only workers with this status
 * @param roleId only workers with this role
 * @param search text contained in the name or in the document, ignoring case
 */
public record WorkerFilter(Boolean active, UUID roleId, String search) {

    /** True when {@code search} has text to look for. */
    public boolean hasSearch() {
        return search != null && !search.isBlank();
    }
}
