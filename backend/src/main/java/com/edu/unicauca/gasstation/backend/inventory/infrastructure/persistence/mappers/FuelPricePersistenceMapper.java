package com.edu.unicauca.gasstation.backend.inventory.infrastructure.persistence.mappers;

import com.edu.unicauca.gasstation.backend.inventory.domain.models.FuelPrice;
import com.edu.unicauca.gasstation.backend.inventory.infrastructure.persistence.entities.FuelPriceEntity;
import org.springframework.stereotype.Component;

@Component
public class FuelPricePersistenceMapper {

    public FuelPrice toDomain(FuelPriceEntity entity) {
        FuelPrice price = new FuelPrice();
        price.setId(entity.getId());
        price.setFuelType(entity.getFuelType());
        price.setPricePerGallon(entity.getPricePerGallon());
        price.setValidFrom(entity.getValidFrom());
        return price;
    }

    public FuelPriceEntity toEntity(FuelPrice price) {
        FuelPriceEntity entity = new FuelPriceEntity();
        entity.setId(price.getId());
        entity.setFuelType(price.getFuelType());
        entity.setPricePerGallon(price.getPricePerGallon());
        entity.setValidFrom(price.getValidFrom());
        return entity;
    }
}
