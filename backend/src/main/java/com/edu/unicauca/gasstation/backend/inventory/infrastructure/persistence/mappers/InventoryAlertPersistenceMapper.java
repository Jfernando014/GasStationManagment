package com.edu.unicauca.gasstation.backend.inventory.infrastructure.persistence.mappers;

import com.edu.unicauca.gasstation.backend.inventory.domain.models.InventoryAlert;
import com.edu.unicauca.gasstation.backend.inventory.infrastructure.persistence.entities.InventoryAlertEntity;
import org.springframework.stereotype.Component;

@Component
public class InventoryAlertPersistenceMapper {

    public InventoryAlert toDomain(InventoryAlertEntity entity) {
        InventoryAlert alert = new InventoryAlert();
        alert.setId(entity.getId());
        alert.setAlertType(entity.getAlertType());
        alert.setFuelType(entity.getFuelType());
        alert.setLevel(entity.getLevel());
        alert.setStatus(entity.getStatus());
        alert.setAutonomyDays(entity.getAutonomyDays());
        alert.setOpenedOn(entity.getOpenedOn());
        alert.setCreatedAt(entity.getCreatedAt());
        alert.setReadAt(entity.getReadAt());
        alert.setAttendedAt(entity.getAttendedAt());
        alert.setAttendNote(entity.getAttendNote());
        alert.setResolvedAt(entity.getResolvedAt());
        return alert;
    }

    public InventoryAlertEntity toEntity(InventoryAlert alert) {
        InventoryAlertEntity entity = new InventoryAlertEntity();
        entity.setId(alert.getId());
        entity.setAlertType(alert.getAlertType());
        entity.setFuelType(alert.getFuelType());
        entity.setLevel(alert.getLevel());
        entity.setStatus(alert.getStatus());
        entity.setAutonomyDays(alert.getAutonomyDays());
        entity.setOpenedOn(alert.getOpenedOn());
        entity.setCreatedAt(alert.getCreatedAt());
        entity.setReadAt(alert.getReadAt());
        entity.setAttendedAt(alert.getAttendedAt());
        entity.setAttendNote(alert.getAttendNote());
        entity.setResolvedAt(alert.getResolvedAt());
        return entity;
    }
}