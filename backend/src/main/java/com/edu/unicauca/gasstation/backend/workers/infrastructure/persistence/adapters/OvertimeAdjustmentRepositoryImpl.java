package com.edu.unicauca.gasstation.backend.workers.infrastructure.persistence.adapters;

import com.edu.unicauca.gasstation.backend.workers.domain.models.AdjustmentDirection;
import com.edu.unicauca.gasstation.backend.workers.domain.models.OvertimeAdjustment;
import com.edu.unicauca.gasstation.backend.workers.domain.models.OvertimeCategory;
import com.edu.unicauca.gasstation.backend.workers.domain.repositories.OvertimeAdjustmentRepository;
import com.edu.unicauca.gasstation.backend.workers.infrastructure.persistence.entities.OvertimeAdjustmentEntity;
import com.edu.unicauca.gasstation.backend.workers.infrastructure.persistence.repositories.JpaOvertimeAdjustmentRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/** JPA implementation of {@link OvertimeAdjustmentRepository}. */
@Repository
@RequiredArgsConstructor
public class OvertimeAdjustmentRepositoryImpl implements OvertimeAdjustmentRepository {

    private final JpaOvertimeAdjustmentRepository repository;

    @Override
    @Transactional
    public OvertimeAdjustment save(OvertimeAdjustment adjustment) {
        return toDomain(repository.saveAndFlush(toEntity(adjustment)));
    }

    @Override
    public Optional<OvertimeAdjustment> findById(Long id) {
        return repository.findById(id).map(OvertimeAdjustmentRepositoryImpl::toDomain);
    }

    @Override
    public List<OvertimeAdjustment> findByWorkerAndFortnight(UUID workerId, LocalDate fortnightStart) {
        return repository.findByWorkerIdAndFortnightStartOrderByCreatedAtAscIdAsc(workerId, fortnightStart).stream()
                .map(OvertimeAdjustmentRepositoryImpl::toDomain)
                .toList();
    }

    @Override
    public List<OvertimeAdjustment> findByFortnight(LocalDate fortnightStart) {
        return repository.findByFortnightStartOrderByCreatedAtAscIdAsc(fortnightStart).stream()
                .map(OvertimeAdjustmentRepositoryImpl::toDomain)
                .toList();
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        repository.deleteById(id);
    }

    private static OvertimeAdjustment toDomain(OvertimeAdjustmentEntity entity) {
        return new OvertimeAdjustment(entity.getId(), entity.getWorkerId(), entity.getFortnightStart(),
                OvertimeCategory.valueOf(entity.getCategory()), AdjustmentDirection.valueOf(entity.getDirection()),
                entity.getHours(), entity.getReason(), entity.getCreatedAt(), entity.getCreatedBy());
    }

    private static OvertimeAdjustmentEntity toEntity(OvertimeAdjustment adjustment) {
        OvertimeAdjustmentEntity entity = new OvertimeAdjustmentEntity();
        entity.setId(adjustment.id());
        entity.setWorkerId(adjustment.workerId());
        entity.setFortnightStart(adjustment.fortnightStart());
        entity.setCategory(adjustment.category().name());
        entity.setDirection(adjustment.direction().name());
        entity.setHours(adjustment.hours());
        entity.setReason(adjustment.reason());
        entity.setCreatedAt(adjustment.createdAt());
        entity.setCreatedBy(adjustment.createdBy());
        return entity;
    }
}
