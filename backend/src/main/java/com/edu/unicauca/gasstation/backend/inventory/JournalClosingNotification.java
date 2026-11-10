package com.edu.unicauca.gasstation.backend.inventory;

import java.time.LocalDate;

/**
 * Inbound contract consumed by the {@code inventory} module when an operational journal is closed.
 *
 * <p>The {@code sales} module's {@code JournalClosedEvent} implements this interface, so {@code inventory}
 * can listen to the event without importing {@code sales}. That keeps the dependency direction
 * {@code sales -> inventory} and avoids a module cycle, since {@code sales} already depends on
 * {@code inventory} for fuel prices, sales summaries and history.
 *
 * <p>This is the consumer-owned contract of the event: the payload is unchanged, only the type the
 * listener observes belongs to the consumer.
 */
public interface JournalClosingNotification {

    /**
     * Operational date of the journal that was closed.
     */
    LocalDate journalDate();
}
