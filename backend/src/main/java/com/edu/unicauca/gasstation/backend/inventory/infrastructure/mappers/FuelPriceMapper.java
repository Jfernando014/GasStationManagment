package com.edu.unicauca.gasstation.backend.inventory.infrastructure.mappers;

import com.edu.unicauca.gasstation.backend.inventory.api.dto.FuelPriceRequest;
import com.edu.unicauca.gasstation.backend.inventory.api.dto.FuelPriceResponse;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.FuelPrice;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface FuelPriceMapper {

    @Mapping(target = "id", ignore = true)
    FuelPrice toEntity(FuelPriceRequest request);

    FuelPriceResponse toResponse(FuelPrice price);
}