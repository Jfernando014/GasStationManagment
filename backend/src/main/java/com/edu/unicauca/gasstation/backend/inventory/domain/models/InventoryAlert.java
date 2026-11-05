package com.edu.unicauca.gasstation.backend.inventory.domain.models;

import com.edu.unicauca.gasstation.backend.inventory.FuelType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class InventoryAlert {

    private Long id;
    private AlertType alertType;
    private FuelType fuelType;
    private AutonomyLevel level;
    private AlertStatus status;
    private BigDecimal autonomyDays;
    private LocalDate openedOn;
    private LocalDateTime createdAt;
    private LocalDateTime readAt;
    private LocalDateTime attendedAt;
    private String attendNote;
    private LocalDateTime resolvedAt;

    public boolean isOpen() {
        return status.isOpen();
    }
}