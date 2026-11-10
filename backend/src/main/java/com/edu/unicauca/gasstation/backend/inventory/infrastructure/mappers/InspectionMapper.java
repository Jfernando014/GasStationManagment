package com.edu.unicauca.gasstation.backend.inventory.infrastructure.mappers;

import com.edu.unicauca.gasstation.backend.inventory.api.dto.InspectionRequest;
import com.edu.unicauca.gasstation.backend.inventory.api.dto.InspectionResponse;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.Inspection;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface InspectionMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Inspection toDomain(InspectionRequest request);

    InspectionResponse toResponse(Inspection inspection);

    List<InspectionResponse> toResponses(List<Inspection> inspections);
}
