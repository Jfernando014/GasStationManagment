package com.edu.unicauca.gasstation.backend.inventory.domain.models;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class InventoryHome {

    private final List<FuelStockSummary> fuels;
    private final List<TankSnapshot> tanks;
    private final InventoryAlert mostCriticalAlert;
    private final long pendingAlerts;
}