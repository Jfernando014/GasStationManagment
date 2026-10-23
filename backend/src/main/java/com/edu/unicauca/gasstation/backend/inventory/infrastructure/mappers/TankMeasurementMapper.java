package com.edu.unicauca.gasstation.backend.inventory.infrastructure.mappers;

import com.edu.unicauca.gasstation.backend.inventory.api.dto.TankMeasurementResponse;
import com.edu.unicauca.gasstation.backend.inventory.api.dto.TankReadingRequest;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.TankMeasurement;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.TankReading;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface TankMeasurementMapper {

    @Mapping(target = "volumeGallons", ignore = true)
    TankReading toReading(TankReadingRequest request);

    List<TankReading> toReadings(List<TankReadingRequest> requests);

    TankMeasurementResponse toResponse(TankMeasurement measurement);
}