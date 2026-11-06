package com.edu.unicauca.gasstation.backend.inventory.api.dto;

import com.edu.unicauca.gasstation.backend.inventory.FuelType;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.AlertStatus;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.AlertType;
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
@Schema(description = "Alert about the autonomy of a fuel")
public class InventoryAlertResponse {

    private Long id;
    private AlertType alertType;
    private FuelType fuelType;

    @Schema(description = "WARNING or CRITICAL")
    private AutonomyLevel level;

    @Schema(description = "ACTIVE, READ, ATTENDED or RESOLVED (the condition is over)")
    private AlertStatus status;

    @Schema(description = "Days of autonomy at the last evaluation")
    private BigDecimal autonomyDays;

    @Schema(description = "Journal date on which the condition appeared")
    private LocalDate openedOn;

    private LocalDateTime createdAt;
    private LocalDateTime readAt;
    private LocalDateTime attendedAt;
    private String attendNote;
    private LocalDateTime resolvedAt;
}