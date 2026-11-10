package com.edu.unicauca.gasstation.backend.inventory;

import java.time.LocalDate;

/**
 * Public read query that exposes the inventory state of a journal date to other modules (S3-19).
 */
public interface InventoryClosingQuery {

    /**
     * Returns the variations, the tank measurement flag and the alerts opened on the given date.
     * An empty snapshot is returned when there is nothing to publish.
     */
    InventoryClosingSnapshot findClosingSnapshot(LocalDate journalDate);
}
