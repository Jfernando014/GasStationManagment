package com.edu.unicauca.gasstation.backend.inventory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

/**
 * Gallons sold per fuel on a journal. Fuels without sales data are absent from the map.
 */
public record SalesSummary(LocalDate journalDate, Map<FuelType, BigDecimal> gallonsByFuel) {

    public SalesSummary {
        gallonsByFuel = Map.copyOf(gallonsByFuel);
    }
}