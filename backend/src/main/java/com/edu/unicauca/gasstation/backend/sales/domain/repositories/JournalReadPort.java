package com.edu.unicauca.gasstation.backend.sales.domain.repositories;

import com.edu.unicauca.gasstation.backend.sales.domain.JournalSnapshot;
import java.util.Optional;

public interface JournalReadPort {

    Optional<JournalSnapshot> findSnapshotById(Long journalId);
}