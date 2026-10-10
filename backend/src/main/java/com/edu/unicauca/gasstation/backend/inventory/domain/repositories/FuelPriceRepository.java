package com.edu.unicauca.gasstation.backend.inventory.domain.repositories;

import com.edu.unicauca.gasstation.backend.inventory.FuelType;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.FuelPrice;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface FuelPriceRepository {

    FuelPrice save(FuelPrice price);

    Optional<FuelPrice> findById(Long id);

    Optional<FuelPrice> findEffective(FuelType fuelType, LocalDate date);

    List<FuelPrice> findHistory(FuelType fuelType);

    List<FuelPrice> findHistory();
}
