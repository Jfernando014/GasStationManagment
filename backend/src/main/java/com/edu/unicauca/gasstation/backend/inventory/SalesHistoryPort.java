package com.edu.unicauca.gasstation.backend.inventory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

/**
 * Contract owned by Inventory and implemented by the Sales module.
 */
public interface SalesHistoryPort {

    /**
     * Average gallons sold per fuel on the same day of the week as {@code journalDate},
     * over the {@code weeks} most recent occurrences strictly before it.
     * Days without sales data are ignored; fuels with no data at all are absent from the map.
     */
    Map<FuelType, BigDecimal> averageGallonsForSameWeekday(LocalDate journalDate, int weeks);
}