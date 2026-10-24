package com.edu.unicauca.gasstation.backend.sales.domain.repositories;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;

/**
 * Secondary port for consuming effective fuel prices for an operational date (Inventory module).
 * Decouples sales domain from internal inventory entities while consuming fuelPriceQuery.
 */
public interface FuelPriceReadPort {

    Optional<BigDecimal> findEffectivePrice(String fuelType, LocalDate date);

    Map<String, BigDecimal> findEffectivePrices(LocalDate date);
}
