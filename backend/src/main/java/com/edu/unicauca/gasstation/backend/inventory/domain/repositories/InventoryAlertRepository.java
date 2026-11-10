package com.edu.unicauca.gasstation.backend.inventory.domain.repositories;

import com.edu.unicauca.gasstation.backend.inventory.FuelType;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.AlertStatus;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.AlertType;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.InventoryAlert;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface InventoryAlertRepository {
    InventoryAlert save(InventoryAlert alert);

    Optional<InventoryAlert> findById(Long id);

    Optional<InventoryAlert> findOpen(AlertType alertType, FuelType fuelType);

    List<InventoryAlert> findByStatuses(Collection<AlertStatus> statuses);

    List<InventoryAlert> findByOpenedOn(LocalDate openedOn);

    long countByStatuses(Collection<AlertStatus> statuses);
}