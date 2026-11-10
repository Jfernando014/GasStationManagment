package com.edu.unicauca.gasstation.backend.incentives.infrastructure.persistence.repositories;

import com.edu.unicauca.gasstation.backend.incentives.domain.models.SyncStatus;
import com.edu.unicauca.gasstation.backend.incentives.infrastructure.persistence.entities.IncentiveJournalSyncEntity;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data repository used by {@code JournalSyncRepositoryImpl}. */
public interface JpaJournalSyncRepository extends JpaRepository<IncentiveJournalSyncEntity, LocalDate> {

    List<IncentiveJournalSyncEntity> findByStatusOrderByJournalDateAsc(SyncStatus status);
}
