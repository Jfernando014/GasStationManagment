package com.edu.unicauca.gasstation.backend.sales.domain.profit;

import com.edu.unicauca.gasstation.backend.inventory.FuelType;
import java.math.BigDecimal;
import java.util.List;

/**
 * Month result of a fuel. Null means that the figure cannot be calculated.
 *
 * @param complete false when a day with gallons lacked a figure; see {@code missingData}
 * @param referenceValues figures in force on the reference date of the report
 */
public record FuelProfit(
        FuelType fuelType,
        boolean complete,
        List<MissingData> missingData,
        BigDecimal gallons,
        BigDecimal sales,
        BigDecimal realProfit,
        BigDecimal targetGallons,
        BigDecimal compliancePercentage,
        BigDecimal targetProfit,
        BigDecimal referenceMarginPerGallon,
        BigDecimal commission,
        BigDecimal profitAfterCommission,
        FuelUnitValues referenceValues,
        List<FuelDayDetail> days) {
}
