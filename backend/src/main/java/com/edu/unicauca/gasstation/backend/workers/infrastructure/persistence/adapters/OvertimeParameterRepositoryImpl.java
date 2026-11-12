package com.edu.unicauca.gasstation.backend.workers.infrastructure.persistence.adapters;

import com.edu.unicauca.gasstation.backend.workers.domain.models.OvertimeParameters;
import com.edu.unicauca.gasstation.backend.workers.domain.repositories.OvertimeParameterRepository;
import com.edu.unicauca.gasstation.backend.workers.exception.ParameterVersionExistsException;
import com.edu.unicauca.gasstation.backend.workers.infrastructure.persistence.entities.OvertimeParameterEntity;
import com.edu.unicauca.gasstation.backend.workers.infrastructure.persistence.repositories.JpaOvertimeParameterRepository;
import java.util.HashSet;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/** JPA implementation of {@link OvertimeParameterRepository}. */
@Repository
@RequiredArgsConstructor
public class OvertimeParameterRepositoryImpl implements OvertimeParameterRepository {

    private final JpaOvertimeParameterRepository repository;

    @Override
    @Transactional(readOnly = true)
    public List<OvertimeParameters> findAll() {
        return repository.findAllByOrderByValidFromAsc().stream().map(OvertimeParameterRepositoryImpl::toDomain)
                .toList();
    }

    @Override
    @Transactional
    public OvertimeParameters save(OvertimeParameters parameters) {
        try {
            // Flushed now so a concurrent duplicate surfaces here as a conflict
            return toDomain(repository.saveAndFlush(toEntity(parameters)));
        } catch (DataIntegrityViolationException e) {
            String cause = e.getMostSpecificCause().getMessage();
            if (cause != null && cause.contains(OvertimeParameterEntity.UNIQUE_VALID_FROM_CONSTRAINT)) {
                throw new ParameterVersionExistsException(parameters.validFrom());
            }
            throw e;
        }
    }

    private static OvertimeParameters toDomain(OvertimeParameterEntity entity) {
        return new OvertimeParameters(entity.getId(), entity.getValidFrom(), entity.getNightStartHour(),
                entity.getRestHours(), entity.getRestMinShiftHours(), entity.getFixedHedoHours(),
                entity.getFixedHedoRoles(), entity.getReason(), entity.getCreatedAt(), entity.getCreatedBy());
    }

    private static OvertimeParameterEntity toEntity(OvertimeParameters parameters) {
        OvertimeParameterEntity entity = new OvertimeParameterEntity();
        entity.setValidFrom(parameters.validFrom());
        entity.setNightStartHour(parameters.nightStartHour());
        entity.setRestHours(parameters.restHours());
        entity.setRestMinShiftHours(parameters.restMinShiftHours());
        entity.setFixedHedoHours(parameters.fixedHedoHours());
        entity.setFixedHedoRoles(new HashSet<>(parameters.fixedHedoRoles()));
        entity.setReason(parameters.reason());
        entity.setCreatedAt(parameters.createdAt());
        entity.setCreatedBy(parameters.createdBy());
        return entity;
    }
}
