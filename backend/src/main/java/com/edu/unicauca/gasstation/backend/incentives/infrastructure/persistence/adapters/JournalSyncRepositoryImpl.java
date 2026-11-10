package com.edu.unicauca.gasstation.backend.incentives.infrastructure.persistence.adapters;

import com.edu.unicauca.gasstation.backend.incentives.domain.models.JournalSync;
import com.edu.unicauca.gasstation.backend.incentives.domain.models.SyncStatus;
import com.edu.unicauca.gasstation.backend.incentives.domain.repositories.JournalSyncRepository;
import com.edu.unicauca.gasstation.backend.incentives.infrastructure.persistence.entities.IncentiveJournalSyncEntity;
import com.edu.unicauca.gasstation.backend.incentives.infrastructure.persistence.repositories.JpaJournalSyncRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/** JPA implementation of {@link JournalSyncRepository}. */
@Repository
@RequiredArgsConstructor
public class JournalSyncRepositoryImpl implements JournalSyncRepository {

    private final JpaJournalSyncRepository repository;

    @Override
    @Transactional
    public JournalSync save(JournalSync sync) {
        return toDomain(repository.saveAndFlush(toEntity(sync)));
    }

    @Override
    public Optional<JournalSync> findByDate(LocalDate journalDate) {
        return repository.findById(journalDate).map(JournalSyncRepositoryImpl::toDomain);
    }

    @Override
    public List<JournalSync> findPending() {
        return repository.findByStatusOrderByJournalDateAsc(SyncStatus.PENDING).stream()
                .map(JournalSyncRepositoryImpl::toDomain)
                .toList();
    }

    private static JournalSync toDomain(IncentiveJournalSyncEntity entity) {
        return new JournalSync(entity.getJournalDate(), entity.getJournalId(), entity.getConsecutive(),
                entity.getClosedAt(), entity.getStatus(), entity.getAttempts(), entity.getLastError(),
                entity.getProcessedAt(), entity.getUpdatedAt());
    }

    private static IncentiveJournalSyncEntity toEntity(JournalSync sync) {
        IncentiveJournalSyncEntity entity = new IncentiveJournalSyncEntity();
        entity.setJournalDate(sync.journalDate());
        entity.setJournalId(sync.journalId());
        entity.setConsecutive(sync.consecutive());
        entity.setClosedAt(sync.closedAt());
        entity.setStatus(sync.status());
        entity.setAttempts(sync.attempts());
        entity.setLastError(sync.lastError());
        entity.setProcessedAt(sync.processedAt());
        entity.setUpdatedAt(sync.updatedAt());
        return entity;
    }
}
