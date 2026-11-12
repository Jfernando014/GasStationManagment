package com.edu.unicauca.gasstation.backend.workers.infrastructure.persistence.adapters;

import com.edu.unicauca.gasstation.backend.workers.domain.models.HoursSync;
import com.edu.unicauca.gasstation.backend.workers.domain.models.SyncStatus;
import com.edu.unicauca.gasstation.backend.workers.domain.repositories.HoursSyncRepository;
import com.edu.unicauca.gasstation.backend.workers.infrastructure.persistence.entities.WorkerHoursSyncEntity;
import com.edu.unicauca.gasstation.backend.workers.infrastructure.persistence.repositories.JpaWorkerHoursSyncRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/** JPA implementation of {@link HoursSyncRepository}. */
@Repository
@RequiredArgsConstructor
public class HoursSyncRepositoryImpl implements HoursSyncRepository {

    private final JpaWorkerHoursSyncRepository repository;

    @Override
    @Transactional
    public HoursSync save(HoursSync sync) {
        return toDomain(repository.saveAndFlush(toEntity(sync)));
    }

    @Override
    public Optional<HoursSync> findByDate(LocalDate workDate) {
        return repository.findById(workDate).map(HoursSyncRepositoryImpl::toDomain);
    }

    @Override
    public List<HoursSync> findPending() {
        return repository.findByStatusOrderByWorkDateAsc(SyncStatus.PENDING).stream()
                .map(HoursSyncRepositoryImpl::toDomain)
                .toList();
    }

    @Override
    public List<LocalDate> findPendingDatesBetween(LocalDate from, LocalDate to) {
        return repository.findByStatusAndWorkDateBetweenOrderByWorkDateAsc(SyncStatus.PENDING, from, to).stream()
                .map(WorkerHoursSyncEntity::getWorkDate)
                .toList();
    }

    private static HoursSync toDomain(WorkerHoursSyncEntity entity) {
        return new HoursSync(entity.getWorkDate(), entity.getJournalId(), entity.getConsecutive(),
                entity.getClosedAt(), entity.getStatus(), entity.isNoveltiesApplied(), entity.getAttempts(),
                entity.getLastError(), entity.getProcessedAt(), entity.getUpdatedAt());
    }

    private static WorkerHoursSyncEntity toEntity(HoursSync sync) {
        WorkerHoursSyncEntity entity = new WorkerHoursSyncEntity();
        entity.setWorkDate(sync.workDate());
        entity.setJournalId(sync.journalId());
        entity.setConsecutive(sync.consecutive());
        entity.setClosedAt(sync.closedAt());
        entity.setStatus(sync.status());
        entity.setNoveltiesApplied(sync.noveltiesApplied());
        entity.setAttempts(sync.attempts());
        entity.setLastError(sync.lastError());
        entity.setProcessedAt(sync.processedAt());
        entity.setUpdatedAt(sync.updatedAt());
        return entity;
    }
}
