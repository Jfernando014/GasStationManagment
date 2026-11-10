package com.edu.unicauca.gasstation.backend.inventory.infrastructure.persistence.mappers;

import com.edu.unicauca.gasstation.backend.inventory.domain.models.Inspection;
import com.edu.unicauca.gasstation.backend.inventory.infrastructure.persistence.entities.InspectionEntity;
import org.springframework.stereotype.Component;

@Component
public class InspectionPersistenceMapper {

    public InspectionEntity toEntity(Inspection inspection) {
        InspectionEntity entity = new InspectionEntity();
        entity.setId(inspection.getId());
        entity.setCode(inspection.getCode());
        entity.setCategory(inspection.getCategory());
        entity.setTarget(inspection.getTarget());
        entity.setDateExecuted(inspection.getDateExecuted());
        entity.setDateDue(inspection.getDateDue());
        entity.setCertifier(inspection.getCertifier());
        entity.setToleranceMeasured(inspection.getToleranceMeasured());
        entity.setCertDoc(inspection.getCertDoc());
        entity.setCreatedAt(inspection.getCreatedAt());
        entity.setUpdatedAt(inspection.getUpdatedAt());
        return entity;
    }

    public Inspection toDomain(InspectionEntity entity) {
        Inspection inspection = new Inspection();
        inspection.setId(entity.getId());
        inspection.setCode(entity.getCode());
        inspection.setCategory(entity.getCategory());
        inspection.setTarget(entity.getTarget());
        inspection.setDateExecuted(entity.getDateExecuted());
        inspection.setDateDue(entity.getDateDue());
        inspection.setCertifier(entity.getCertifier());
        inspection.setToleranceMeasured(entity.getToleranceMeasured());
        inspection.setCertDoc(entity.getCertDoc());
        inspection.setCreatedAt(entity.getCreatedAt());
        inspection.setUpdatedAt(entity.getUpdatedAt());
        return inspection;
    }
}
