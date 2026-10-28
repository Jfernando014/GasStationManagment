package com.edu.unicauca.gasstation.backend.incentives.domain.ports;

import java.time.LocalDate;
import java.util.Optional;

/** Closed journals, to be served by sales. Until then a temporary one that reports none is used. */
public interface ClosedJournalPort {

    /** Date of the latest closed journal; empty if none has been closed. */
    Optional<LocalDate> lastClosedDate();
}
