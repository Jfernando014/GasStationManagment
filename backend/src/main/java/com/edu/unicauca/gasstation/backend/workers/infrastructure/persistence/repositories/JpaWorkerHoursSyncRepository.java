package com.edu.unicauca.gasstation.backend.workers.infrastructure.persistence.repositories;

import com.edu.unicauca.gasstation.backend.workers.domain.models.SyncStatus;
import com.edu.unicauca.gasstation.backend.workers.infrastructure.persistence.entities.WorkerHoursSyncEntity;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data repository used by {@code HoursSyncRepositoryImpl}. */
public interface JpaWorkerHoursSyncRepository extends JpaRepository<WorkerHoursSyncEntity, LocalDate> {

    List<WorkerHoursSyncEntity> findByStatusOrderByWorkDateAsc(SyncStatus status);

    List<WorkerHoursSyncEntity> findByStatusAndWorkDateBetweenOrderByWorkDateAsc(SyncStatus status, LocalDate from,
                                                                                 LocalDate to);
}
