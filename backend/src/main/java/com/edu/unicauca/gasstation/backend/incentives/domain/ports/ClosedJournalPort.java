package com.edu.unicauca.gasstation.backend.incentives.domain.ports;

import java.time.LocalDate;
import java.util.Optional;

/** Closed journals, as this module knows them from the closings it has processed. */
public interface ClosedJournalPort {

    /** Date of the latest closed journal, processed or still pending; empty if none has been closed. */
    Optional<LocalDate> lastClosedDate();
}
