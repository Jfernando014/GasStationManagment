package com.edu.unicauca.gasstation.backend.inventory.infrastructure.persistence.mappers;

import com.edu.unicauca.gasstation.backend.inventory.domain.models.AutonomyThreshold;
import com.edu.unicauca.gasstation.backend.inventory.infrastructure.persistence.entities.AutonomyThresholdEntity;
import org.springframework.stereotype.Component;

@Component
public class AutonomyThresholdPersistenceMapper {

    public AutonomyThreshold toDomain(AutonomyThresholdEntity entity) {
        AutonomyThreshold threshold = new AutonomyThreshold();
        threshold.setId(entity.getId());
        threshold.setValidFrom(entity.getValidFrom());
        threshold.setWarningDays(entity.getWarningDays());
        threshold.setCriticalDays(entity.getCriticalDays());
        threshold.setCreatedAt(entity.getCreatedAt());
        return threshold;
    }

    public AutonomyThresholdEntity toEntity(AutonomyThreshold threshold) {
        AutonomyThresholdEntity entity = new AutonomyThresholdEntity();
        entity.setId(threshold.getId());
        entity.setValidFrom(threshold.getValidFrom());
        entity.setWarningDays(threshold.getWarningDays());
        entity.setCriticalDays(threshold.getCriticalDays());
        entity.setCreatedAt(threshold.getCreatedAt());
        return entity;
    }
}