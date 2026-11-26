package com.edu.unicauca.gasstation.backend.sales.domain.repositories;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

/**
 * Secondary port for consuming the fuel purchase cost per gallon in force on a date (Inventory module).
 */
public interface FuelPurchasePriceReadPort {

    Optional<BigDecimal> findEffectivePrice(String fuelType, LocalDate date);
}
