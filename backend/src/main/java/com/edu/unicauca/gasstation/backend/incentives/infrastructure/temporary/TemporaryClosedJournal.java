package com.edu.unicauca.gasstation.backend.incentives.infrastructure.temporary;

import com.edu.unicauca.gasstation.backend.incentives.domain.ports.ClosedJournalPort;
import java.time.LocalDate;
import java.util.Optional;

/** TEMPORARY. Reports no closed journal (or a configured date) until sales publishes its own. */
class TemporaryClosedJournal implements ClosedJournalPort {

    private final LocalDate lastClosedDate;

    TemporaryClosedJournal(LocalDate lastClosedDate) {
        this.lastClosedDate = lastClosedDate;
    }

    @Override
    public Optional<LocalDate> lastClosedDate() {
        return Optional.ofNullable(lastClosedDate);
    }
}
