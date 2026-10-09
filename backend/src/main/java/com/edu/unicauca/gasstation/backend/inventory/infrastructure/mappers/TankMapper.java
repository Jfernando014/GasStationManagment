package com.edu.unicauca.gasstation.backend.inventory.infrastructure.mappers;

import com.edu.unicauca.gasstation.backend.inventory.api.dto.TankRequest;
import com.edu.unicauca.gasstation.backend.inventory.api.dto.TankResponse;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.Tank;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;


@Mapper(componentModel = "spring")
public interface TankMapper {
    @Mapping(target = "id", ignore = true)
    Tank toEntity(TankRequest request);

    TankResponse toResponse(Tank tank);
}
