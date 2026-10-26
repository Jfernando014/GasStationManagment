package com.edu.unicauca.gasstation.backend.inventory.api.dto;

import com.edu.unicauca.gasstation.backend.inventory.FuelType;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.AutonomyLevel;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.AutonomyStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "Days of autonomy of a fuel on a journal date")
public class FuelAutonomyResponse {

    private FuelType fuelType;
    private AutonomyStatus status;

    @Schema(description = "Net gallons measured in all the tanks of the fuel. Null if any tank was not measured")
    private BigDecimal stockGallons;

    @Schema(description = "Average gallons sold on the same day of the week")
    private BigDecimal expectedDailySalesGallons;

    private BigDecimal autonomyDays;

    @Schema(description = "WARNING or CRITICAL when the autonomy reaches the threshold. "
            + "NOT_EVALUATED when it could not be calculated or no threshold applies")
    private AutonomyLevel level;

    @Schema(description = "Thresholds in force on the date. Null if none is configured")
    private BigDecimal warningDays;
    private BigDecimal criticalDays;

    @Schema(description = "Measured tanks of the fuel with their level")
    private List<TankStockResponse> tanks;
}