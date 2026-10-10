package com.edu.unicauca.gasstation.backend.inventory.infrastructure.persistence.mappers;

import com.edu.unicauca.gasstation.backend.inventory.domain.models.Tank;
import com.edu.unicauca.gasstation.backend.inventory.infrastructure.persistence.entities.TankEntity;
import org.springframework.stereotype.Component;

@Component
public class TankPersistenceMapper {

    public Tank toDomain(TankEntity entity) {
        Tank tank = new Tank();
        tank.setId(entity.getId());
        tank.setCode(entity.getCode());
        tank.setName(entity.getName());
        tank.setFuelType(entity.getFuelType());
        tank.setMaxCapacityGallons(entity.getMaxCapacityGallons());
        tank.setMaxHeightCm(entity.getMaxHeightCm());
        tank.setToleranceCm(entity.getToleranceCm());
        return tank;
    }

    public TankEntity toEntity(Tank tank) {
        TankEntity entity = new TankEntity();
        entity.setId(tank.getId());
        entity.setCode(tank.getCode());
        entity.setName(tank.getName());
        entity.setFuelType(tank.getFuelType());
        entity.setMaxCapacityGallons(tank.getMaxCapacityGallons());
        entity.setMaxHeightCm(tank.getMaxHeightCm());
        entity.setToleranceCm(tank.getToleranceCm());
        return entity;
    }
}
