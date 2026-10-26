package com.edu.unicauca.gasstation.backend.inventory.infrastructure.persistence.mappers;

import com.edu.unicauca.gasstation.backend.inventory.domain.models.StockMovement;
import com.edu.unicauca.gasstation.backend.inventory.infrastructure.persistence.entities.ProductStockMovementEntity;
import org.springframework.stereotype.Component;

@Component
public class StockMovementPersistenceMapper {

    public StockMovement toDomain(ProductStockMovementEntity entity) {
        StockMovement movement = new StockMovement();
        movement.setId(entity.getId());
        movement.setProductId(entity.getProductId());
        movement.setJournalDate(entity.getJournalDate());
        movement.setLocation(entity.getLocation());
        movement.setMovementType(entity.getMovementType());
        movement.setQuantity(entity.getQuantity());
        movement.setNote(entity.getNote());
        movement.setCreatedAt(entity.getCreatedAt());
        return movement;
    }

    public ProductStockMovementEntity toEntity(StockMovement movement) {
        ProductStockMovementEntity entity = new ProductStockMovementEntity();
        entity.setId(movement.getId());
        entity.setProductId(movement.getProductId());
        entity.setJournalDate(movement.getJournalDate());
        entity.setLocation(movement.getLocation());
        entity.setMovementType(movement.getMovementType());
        entity.setQuantity(movement.getQuantity());
        entity.setNote(movement.getNote());
        entity.setCreatedAt(movement.getCreatedAt());
        return entity;
    }
}