package com.edu.unicauca.gasstation.backend.inventory.infrastructure.mappers;

import com.edu.unicauca.gasstation.backend.inventory.api.dto.FuelAutonomyResponse;
import com.edu.unicauca.gasstation.backend.inventory.api.dto.TankStockResponse;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.FuelAutonomy;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.TankStock;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface FuelAutonomyMapper {

    FuelAutonomyResponse toResponse(FuelAutonomy autonomy);

    List<FuelAutonomyResponse> toResponses(List<FuelAutonomy> autonomies);

    TankStockResponse toResponse(TankStock stock);
}