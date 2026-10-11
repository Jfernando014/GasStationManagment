package com.edu.unicauca.gasstation.backend.sales.infrastructure.persistence.mappers;

import com.edu.unicauca.gasstation.backend.sales.domain.models.*;
import com.edu.unicauca.gasstation.backend.sales.infrastructure.persistence.entities.SalesTargetEntity;
import org.springframework.stereotype.Component;

@Component
public class SalesTargetPersistenceMapper {
    /** Always creates a new row; clients cannot supply database metadata. */
    public SalesTargetEntity toEntity(SalesTarget target) {
        SalesTargetEntity entity = new SalesTargetEntity();
        SalesTargetPeriod period = target.identity().period();
        entity.setTargetYear(period.year());
        entity.setTargetMonth(period.month() == null ? null : period.month().shortValue());
        entity.setCategory(target.identity().category());
        entity.setTargetValue(target.value());
        entity.setUnit(target.unit());
        return entity;
    }

    public StoredSalesTarget toDomain(SalesTargetEntity entity) {
        Integer month = entity.getTargetMonth() == null ? null : entity.getTargetMonth().intValue();
        SalesTargetIdentity identity = new SalesTargetIdentity(
                new SalesTargetPeriod(entity.getTargetYear(), month), entity.getCategory());
        return new StoredSalesTarget(entity.getId(),
                new SalesTarget(identity, entity.getTargetValue(), entity.getUnit()), entity.getCreatedAt());
    }
}
