package com.edu.unicauca.gasstation.backend.inventory;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.Optional;

/**
 * Contract owned by Inventory for the monthly sales target of a fuel, which is administered by the targets feature.
 */
public interface FuelMonthlyTargetPort {

    /**
     * @return the target of the fuel for the month in gallons, or empty if it has none
     */
    Optional<BigDecimal> findTargetGallons(FuelType fuelType, YearMonth month);
}
