package com.edu.unicauca.gasstation.backend.inventory.infrastructure.persistence.mappers;

import com.edu.unicauca.gasstation.backend.inventory.domain.models.FuelPurchasePrice;
import com.edu.unicauca.gasstation.backend.inventory.infrastructure.persistence.entities.FuelPurchasePriceEntity;
import org.springframework.stereotype.Component;

@Component
public class FuelPurchasePricePersistenceMapper {

    public FuelPurchasePrice toDomain(FuelPurchasePriceEntity entity) {
        FuelPurchasePrice price = new FuelPurchasePrice();
        price.setId(entity.getId());
        price.setFuelType(entity.getFuelType());
        price.setPricePerGallon(entity.getPricePerGallon());
        price.setValidFrom(entity.getValidFrom());
        return price;
    }

    public FuelPurchasePriceEntity toEntity(FuelPurchasePrice price) {
        FuelPurchasePriceEntity entity = new FuelPurchasePriceEntity();
        entity.setId(price.getId());
        entity.setFuelType(price.getFuelType());
        entity.setPricePerGallon(price.getPricePerGallon());
        entity.setValidFrom(price.getValidFrom());
        return entity;
    }
}
