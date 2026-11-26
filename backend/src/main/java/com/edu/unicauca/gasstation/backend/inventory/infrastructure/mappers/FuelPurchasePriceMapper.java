package com.edu.unicauca.gasstation.backend.inventory.infrastructure.mappers;

import com.edu.unicauca.gasstation.backend.inventory.api.dto.FuelPurchasePriceRequest;
import com.edu.unicauca.gasstation.backend.inventory.api.dto.FuelPurchasePriceResponse;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.FuelPurchasePrice;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface FuelPurchasePriceMapper {

    @Mapping(target = "id", ignore = true)
    FuelPurchasePrice toEntity(FuelPurchasePriceRequest request);

    FuelPurchasePriceResponse toResponse(FuelPurchasePrice price);
}
