package com.edu.unicauca.gasstation.backend.inventory.infrastructure.persistence.repositories;

import com.edu.unicauca.gasstation.backend.inventory.FuelType;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.AlertStatus;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.AlertType;
import com.edu.unicauca.gasstation.backend.inventory.infrastructure.persistence.entities.InventoryAlertEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface JpaInventoryAlertRepository extends JpaRepository<InventoryAlertEntity, Long> {

    Optional<InventoryAlertEntity> findByAlertTypeAndFuelTypeAndStatusNot(
            AlertType alertType, FuelType fuelType, AlertStatus status);

    List<InventoryAlertEntity> findByStatusInOrderByOpenedOnDescIdDesc(Collection<AlertStatus> statuses);

    List<InventoryAlertEntity> findByOpenedOnOrderByIdAsc(LocalDate openedOn);

    long countByStatusIn(Collection<AlertStatus> statuses);
}