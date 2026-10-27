package com.edu.unicauca.gasstation.backend.inventory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

/**
 * What was sold on a journal (step 2), in Inventory's terms.
 * Fuels without data are absent from {@code gallonsByFuel}. {@code unitsByProduct} is keyed by
 * Inventory's {@code Product.code}; products that were not sold are absent.
 */
public record SalesSummary(LocalDate journalDate,
                           Map<FuelType, BigDecimal> gallonsByFuel,
                           Map<String, Integer> unitsByProduct) {

    public SalesSummary {
        gallonsByFuel = Map.copyOf(gallonsByFuel);
        unitsByProduct = Map.copyOf(unitsByProduct);
    }

    /** Summary without product sales. */
    public SalesSummary(LocalDate journalDate, Map<FuelType, BigDecimal> gallonsByFuel) {
        this(journalDate, gallonsByFuel, Map.of());
    }
}