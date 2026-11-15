package com.edu.unicauca.gasstation.backend.incentives.infrastructure.persistence.repositories;

import com.edu.unicauca.gasstation.backend.incentives.domain.models.SyncStatus;
import com.edu.unicauca.gasstation.backend.incentives.infrastructure.persistence.entities.IncentiveJournalSyncEntity;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/** Spring Data repository used by {@code JournalSyncRepositoryImpl}. */
public interface JpaJournalSyncRepository extends JpaRepository<IncentiveJournalSyncEntity, LocalDate> {

    List<IncentiveJournalSyncEntity> findByStatusOrderByJournalDateAsc(SyncStatus status);

    @Query("select max(sync.journalDate) from IncentiveJournalSyncEntity sync")
    Optional<LocalDate> findLastJournalDate();
}
