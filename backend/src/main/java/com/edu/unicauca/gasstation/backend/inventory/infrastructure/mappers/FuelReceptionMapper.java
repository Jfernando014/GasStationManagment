package com.edu.unicauca.gasstation.backend.inventory.infrastructure.mappers;

import com.edu.unicauca.gasstation.backend.inventory.api.dto.FuelReceptionRequest;
import com.edu.unicauca.gasstation.backend.inventory.api.dto.FuelReceptionResponse;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.FuelReception;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface FuelReceptionMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    FuelReception toDomain(FuelReceptionRequest request);

    FuelReceptionResponse toResponse(FuelReception reception);

    List<FuelReceptionResponse> toResponses(List<FuelReception> receptions);
}