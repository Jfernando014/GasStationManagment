package com.edu.unicauca.gasstation.backend.inventory.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "Inventory data for the home screen")
public class InventoryHomeResponse {

    private List<FuelStockSummaryResponse> fuels;
    private List<TankSnapshotResponse> tanks;

    @Schema(description = "Pending alert for the banner: critical first, then the one with less autonomy. Null if none")
    private InventoryAlertResponse mostCriticalAlert;

    @Schema(description = "Alerts nobody has attended yet")
    private long pendingAlerts;
}