package com.edu.unicauca.gasstation.backend.inventory;

import java.time.LocalDate;
import java.util.Optional;

public interface FuelPurchasePriceQuery {
    Optional<FuelPurchasePriceInfo> findEffectivePrice(FuelType fuelType, LocalDate date);
}
