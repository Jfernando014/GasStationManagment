package com.edu.unicauca.gasstation.backend.inventory.api.dto;

import com.edu.unicauca.gasstation.backend.inventory.FuelType;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.AutonomyLevel;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "A tank with its last measurement. The measurement fields are null if it was never measured")
public class TankSnapshotResponse {

    private Long tankId;
    private String tankCode;
    private FuelType fuelType;
    private BigDecimal heightCm;
    private BigDecimal gallons;
    private BigDecimal capacityGallons;
    private BigDecimal percentage;

    @Schema(description = "Journal date of the measurement that this reading comes from")
    private LocalDate measuredOn;

    private LocalDateTime measuredAt;

    @Schema(description = "Level of the open alert of the tank's fuel. Null if it has none")
    private AutonomyLevel alertLevel;
}