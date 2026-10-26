package com.edu.unicauca.gasstation.backend.sales.domain.repositories;

import com.edu.unicauca.gasstation.backend.sales.domain.JournalSnapshot;
import java.time.LocalDate;
import java.util.Optional;

/**
 * Read-only lookup of a journal by its date. Separate from {@link JournalReadPort} on purpose: that one is
 * implemented as a lambda by existing tests, so adding a second method to it would break them.
 */
public interface JournalDateReadPort {

    Optional<JournalSnapshot> findSnapshotByDate(LocalDate date);
}
