package com.edu.unicauca.gasstation.backend.sales.domain.repositories;

import com.edu.unicauca.gasstation.backend.inventory.FuelType;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.Map;

/**
 * Secondary port for the monthly fuel targets.
 */
public interface FuelMonthlyTargetReader {

    /** Target gallons of the month by fuel. A fuel without a target is absent from the result. */
    Map<FuelType, BigDecimal> findGallonTargets(YearMonth month);
}
