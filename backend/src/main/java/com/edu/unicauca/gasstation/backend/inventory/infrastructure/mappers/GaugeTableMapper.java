package com.edu.unicauca.gasstation.backend.inventory.infrastructure.mappers;

import com.edu.unicauca.gasstation.backend.inventory.api.dto.*;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.GaugePoint;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.GaugeTable;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.Tank;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface GaugeTableMapper {

    @Mapping(target = "id", ignore = true)
    GaugeTable toDomain(GaugeTableRequest request);

    GaugePoint toDomain(GaugePointRequest request);

    GaugePointResponse toResponse(GaugePoint point);

    TankSummaryResponse toSummary(Tank tank);

    default GaugeTableResponse toResponse(GaugeTable table, List<Tank> tanksUsingIt) {
        GaugeTableResponse response = new GaugeTableResponse();
        response.setId(table.getId());
        response.setName(table.getName());
        response.setPoints(table.getPoints().stream().map(this::toResponse).toList());
        response.setMinHeightCm(table.minHeightCm());
        response.setMaxHeightCm(table.maxHeightCm());
        response.setMaxVolumeGallons(table.maxVolumeGallons());
        response.setTanks(tanksUsingIt.stream().map(this::toSummary).toList());
        return response;
    }
}