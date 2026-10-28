package com.edu.unicauca.gasstation.backend.inventory.infrastructure.mappers;

import com.edu.unicauca.gasstation.backend.inventory.api.dto.FuelVariationResponse;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.FuelVariation;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface FuelVariationMapper {

    FuelVariationResponse toResponse(FuelVariation variation);

    List<FuelVariationResponse> toResponses(List<FuelVariation> variations);
}