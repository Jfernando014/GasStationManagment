package com.edu.unicauca.gasstation.backend.inventory.infrastructure.persistence.mappers;

import com.edu.unicauca.gasstation.backend.inventory.domain.models.FuelReception;
import com.edu.unicauca.gasstation.backend.inventory.infrastructure.persistence.entities.FuelReceptionEntity;
import org.springframework.stereotype.Component;

@Component
public class FuelReceptionPersistenceMapper {

    public FuelReceptionEntity toEntity(FuelReception reception) {
        FuelReceptionEntity entity = new FuelReceptionEntity();
        entity.setId(reception.getId());
        entity.setTankId(reception.getTankId());
        entity.setReceptionDate(reception.getReceptionDate());
        entity.setGallons(reception.getGallons());
        entity.setSupplier(reception.getSupplier());
        entity.setCreatedAt(reception.getCreatedAt());
        entity.setUpdatedAt(reception.getUpdatedAt());
        return entity;
    }

    public FuelReception toDomain(FuelReceptionEntity entity) {
        FuelReception reception = new FuelReception();
        reception.setId(entity.getId());
        reception.setTankId(entity.getTankId());
        reception.setReceptionDate(entity.getReceptionDate());
        reception.setGallons(entity.getGallons());
        reception.setSupplier(entity.getSupplier());
        reception.setCreatedAt(entity.getCreatedAt());
        reception.setUpdatedAt(entity.getUpdatedAt());
        return reception;
    }
}