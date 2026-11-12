package com.edu.unicauca.gasstation.backend.incentives.domain.repositories;

import com.edu.unicauca.gasstation.backend.incentives.domain.models.JournalSync;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** Storage of the processing state of each journal date. */
public interface JournalSyncRepository {

    /** Creates the state of the date or replaces it. */
    JournalSync save(JournalSync sync);

    Optional<JournalSync> findByDate(LocalDate journalDate);

    /** The latest date that has a state, whatever it is; empty if none has. */
    Optional<LocalDate> findLastDate();

    /** Dates still pending, oldest first. */
    List<JournalSync> findPending();
}
