package com.edu.unicauca.gasstation.backend.inventory;

import java.time.LocalDate;
import java.util.Optional;

public interface FuelPriceQuery {
    Optional<FuelPriceInfo> findEffectivePrice(FuelType fuelType, LocalDate date);
}
