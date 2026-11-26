package com.edu.unicauca.gasstation.backend.sales.domain.profit;

import com.edu.unicauca.gasstation.backend.inventory.FuelType;
import java.math.BigDecimal;
import java.util.List;

/**
 * Sum of every fuel. A total is null when it cannot be calculated for any of them: a partial sum is never shown
 * as the total.
 */
public record FuelProfitTotals(
        boolean complete,
        List<FuelType> incompleteFuels,
        List<FuelType> fuelsWithoutTarget,
        BigDecimal gallons,
        BigDecimal sales,
        BigDecimal realProfit,
        BigDecimal commission,
        BigDecimal profitAfterCommission,
        BigDecimal targetGallons,
        BigDecimal compliancePercentage,
        BigDecimal targetProfit) {
}
