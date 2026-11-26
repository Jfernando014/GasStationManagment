package com.edu.unicauca.gasstation.backend.sales.infrastructure.adapters;

import com.edu.unicauca.gasstation.backend.inventory.FuelType;
import com.edu.unicauca.gasstation.backend.sales.domain.repositories.FuelMonthlyTargetReader;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.Map;

/**
 * TEMPORARY, development only: invented targets, the same for every month, to check the fuel profit by hand.
 * It only lives in memory and never writes to the database.
 */
class DevStubFuelMonthlyTargetReader implements FuelMonthlyTargetReader {

    private static final Map<FuelType, BigDecimal> EXAMPLE_TARGETS = Map.of(
            FuelType.MOTOR, new BigDecimal("20000"),
            FuelType.DIESEL, new BigDecimal("2000"),
            FuelType.EXTRA, new BigDecimal("500"),
            FuelType.MAX_PRO, new BigDecimal("1000"));

    @Override
    public Map<FuelType, BigDecimal> findGallonTargets(YearMonth month) {
        return EXAMPLE_TARGETS;
    }
}
