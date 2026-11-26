package com.edu.unicauca.gasstation.backend.inventory.domain.repositories;

import com.edu.unicauca.gasstation.backend.inventory.FuelType;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.FuelPurchasePrice;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface FuelPurchasePriceRepository {

    FuelPurchasePrice save(FuelPurchasePrice price);

    Optional<FuelPurchasePrice> findById(Long id);

    Optional<FuelPurchasePrice> findEffective(FuelType fuelType, LocalDate date);

    List<FuelPurchasePrice> findHistory(FuelType fuelType);

    List<FuelPurchasePrice> findHistory();
}
