package com.edu.unicauca.gasstation.backend.inventory.infrastructure.persistence.adapters;

import com.edu.unicauca.gasstation.backend.inventory.FuelType;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.AlertStatus;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.AlertType;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.InventoryAlert;
import com.edu.unicauca.gasstation.backend.inventory.domain.repositories.InventoryAlertRepository;
import com.edu.unicauca.gasstation.backend.inventory.infrastructure.persistence.mappers.InventoryAlertPersistenceMapper;
import com.edu.unicauca.gasstation.backend.inventory.infrastructure.persistence.repositories.JpaInventoryAlertRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class InventoryAlertRepositoryAdapter implements InventoryAlertRepository {

    private final JpaInventoryAlertRepository jpaRepository;
    private final InventoryAlertPersistenceMapper mapper;

    @Override
    public InventoryAlert save(InventoryAlert alert) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(alert)));
    }

    @Override
    public Optional<InventoryAlert> findById(Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<InventoryAlert> findOpen(AlertType alertType, FuelType fuelType) {
        return jpaRepository
                .findByAlertTypeAndFuelTypeAndStatusNot(alertType, fuelType, AlertStatus.RESOLVED)
                .map(mapper::toDomain);
    }

    @Override
    public List<InventoryAlert> findByStatuses(Collection<AlertStatus> statuses) {
        return jpaRepository.findByStatusInOrderByOpenedOnDescIdDesc(statuses).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public long countByStatuses(Collection<AlertStatus> statuses) {
        return jpaRepository.countByStatusIn(statuses);
    }
}