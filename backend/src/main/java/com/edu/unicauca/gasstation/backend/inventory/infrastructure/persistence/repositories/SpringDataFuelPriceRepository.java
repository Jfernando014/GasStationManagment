package com.edu.unicauca.gasstation.backend.inventory.infrastructure.persistence.repositories;

import com.edu.unicauca.gasstation.backend.inventory.FuelType;
import com.edu.unicauca.gasstation.backend.inventory.infrastructure.persistence.entities.FuelPriceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface SpringDataFuelPriceRepository extends JpaRepository<FuelPriceEntity, Long> {

    Optional<FuelPriceEntity> findFirstByFuelTypeAndValidFromLessThanEqualOrderByValidFromDesc(
            FuelType fuelType, LocalDate date);

    List<FuelPriceEntity> findByFuelTypeOrderByValidFromDesc(FuelType fuelType);

    List<FuelPriceEntity> findAllByOrderByFuelTypeAscValidFromDesc();
}
