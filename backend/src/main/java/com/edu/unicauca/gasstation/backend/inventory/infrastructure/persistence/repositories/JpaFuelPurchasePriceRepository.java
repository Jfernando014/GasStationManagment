package com.edu.unicauca.gasstation.backend.inventory.infrastructure.persistence.repositories;

import com.edu.unicauca.gasstation.backend.inventory.FuelType;
import com.edu.unicauca.gasstation.backend.inventory.infrastructure.persistence.entities.FuelPurchasePriceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface JpaFuelPurchasePriceRepository extends JpaRepository<FuelPurchasePriceEntity, Long> {

    Optional<FuelPurchasePriceEntity> findFirstByFuelTypeAndValidFromLessThanEqualOrderByValidFromDesc(
            FuelType fuelType, LocalDate date);

    List<FuelPurchasePriceEntity> findByFuelTypeOrderByValidFromDesc(FuelType fuelType);

    List<FuelPurchasePriceEntity> findAllByOrderByFuelTypeAscValidFromDesc();
}
