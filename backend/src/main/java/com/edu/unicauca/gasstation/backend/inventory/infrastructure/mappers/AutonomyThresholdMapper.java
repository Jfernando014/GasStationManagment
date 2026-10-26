package com.edu.unicauca.gasstation.backend.inventory.infrastructure.mappers;

import com.edu.unicauca.gasstation.backend.inventory.api.dto.AutonomyThresholdRequest;
import com.edu.unicauca.gasstation.backend.inventory.api.dto.AutonomyThresholdResponse;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.AutonomyThreshold;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface AutonomyThresholdMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    AutonomyThreshold toDomain(AutonomyThresholdRequest request);

    AutonomyThresholdResponse toResponse(AutonomyThreshold threshold);

    List<AutonomyThresholdResponse> toResponses(List<AutonomyThreshold> thresholds);
}