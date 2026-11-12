package com.edu.unicauca.gasstation.backend.incentives.infrastructure.persistence.adapters;

import com.edu.unicauca.gasstation.backend.incentives.domain.ports.ClosedJournalPort;
import com.edu.unicauca.gasstation.backend.incentives.domain.repositories.JournalSyncRepository;
import java.time.LocalDate;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Reports the latest closed journal from the processing state this module keeps for each closed date. */
@Component
@RequiredArgsConstructor
public class ClosedJournalAdapter implements ClosedJournalPort {

    private final JournalSyncRepository syncRepository;

    @Override
    public Optional<LocalDate> lastClosedDate() {
        return syncRepository.findLastDate();
    }
}
