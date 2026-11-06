package com.edu.unicauca.gasstation.backend.inventory.infrastructure.mappers;

import com.edu.unicauca.gasstation.backend.inventory.api.dto.FuelStockSummaryResponse;
import com.edu.unicauca.gasstation.backend.inventory.api.dto.InventoryHomeResponse;
import com.edu.unicauca.gasstation.backend.inventory.api.dto.TankSnapshotResponse;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.FuelStockSummary;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.InventoryHome;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.TankSnapshot;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = InventoryAlertMapper.class)
public interface InventoryHomeMapper {

    InventoryHomeResponse toResponse(InventoryHome home);

    FuelStockSummaryResponse toFuelResponse(FuelStockSummary summary);

    TankSnapshotResponse toTankResponse(TankSnapshot snapshot);
}