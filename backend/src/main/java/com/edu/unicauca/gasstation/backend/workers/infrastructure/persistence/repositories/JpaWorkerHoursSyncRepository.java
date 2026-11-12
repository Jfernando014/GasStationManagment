package com.edu.unicauca.gasstation.backend.workers.infrastructure.persistence.repositories;

import com.edu.unicauca.gasstation.backend.workers.domain.models.SyncStatus;
import com.edu.unicauca.gasstation.backend.workers.infrastructure.persistence.entities.WorkerHoursSyncEntity;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/** Spring Data repository used by {@code HoursSyncRepositoryImpl}. */
public interface JpaWorkerHoursSyncRepository extends JpaRepository<WorkerHoursSyncEntity, LocalDate> {

    List<WorkerHoursSyncEntity> findByStatusOrderByWorkDateAsc(SyncStatus status);

    List<WorkerHoursSyncEntity> findByWorkDateBetweenOrderByWorkDateAsc(LocalDate from, LocalDate to);

    @Query("select max(sync.workDate) from WorkerHoursSyncEntity sync")
    Optional<LocalDate> findLastWorkDate();

    List<WorkerHoursSyncEntity> findByStatusAndWorkDateBetweenOrderByWorkDateAsc(SyncStatus status, LocalDate from,
                                                                                 LocalDate to);
}
