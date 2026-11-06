package com.edu.unicauca.gasstation.backend.inventory.infrastructure.mappers;

import com.edu.unicauca.gasstation.backend.inventory.api.dto.InventoryAlertResponse;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.InventoryAlert;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface InventoryAlertMapper {

    InventoryAlertResponse toResponse(InventoryAlert alert);

    List<InventoryAlertResponse> toResponses(List<InventoryAlert> alerts);
}