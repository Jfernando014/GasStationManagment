package com.edu.unicauca.gasstation.backend.inventory;

import com.edu.unicauca.gasstation.backend.inventory.domain.models.AlertStatus;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.AlertType;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.AutonomyLevel;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.AutonomyStatus;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.FuelAutonomy;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.InventoryAlert;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public final class AlertTestData {

    private AlertTestData() {
    }

    /** An alert whose dates are consistent with its status (so it also satisfies the table CHECKs). */
    public static InventoryAlert alert(Long id, FuelType fuelType, AutonomyLevel level, AlertStatus status,
                                       String days, LocalDate openedOn) {
        InventoryAlert alert = new InventoryAlert();
        alert.setId(id);
        alert.setAlertType(AlertType.AUTONOMY);
        alert.setFuelType(fuelType);
        alert.setLevel(level);
        alert.setStatus(status);
        alert.setAutonomyDays(new BigDecimal(days));
        alert.setOpenedOn(openedOn);
        LocalDateTime created = openedOn.atTime(10, 0);
        alert.setCreatedAt(created);
        if (status != AlertStatus.ACTIVE) {
            alert.setReadAt(created.plusHours(1));
        }
        if (status == AlertStatus.ATTENDED) {
            alert.setAttendedAt(created.plusHours(2));
            alert.setAttendNote("Fuel ordered");
        }
        if (status == AlertStatus.RESOLVED) {
            alert.setResolvedAt(created.plusDays(1));
        }
        return alert;
    }

    public static FuelAutonomy autonomy(FuelType fuelType, AutonomyStatus status, AutonomyLevel level, String days) {
        return FuelAutonomy.builder()
                .fuelType(fuelType)
                .status(status)
                .level(level)
                .autonomyDays(days == null ? null : new BigDecimal(days))
                .build();
    }
}