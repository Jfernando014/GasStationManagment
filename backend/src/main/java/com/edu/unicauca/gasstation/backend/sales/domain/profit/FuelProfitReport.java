package com.edu.unicauca.gasstation.backend.sales.domain.profit;

import java.time.LocalDate;
import java.util.List;

/**
 * Fuel profit of a month.
 *
 * @param month yyyy-MM
 * @param referenceDate day of the reference margin: the last of the month, or today while it is in progress
 * @param daysWithData days of the month that have sales data
 */
public record FuelProfitReport(
        String month,
        LocalDate referenceDate,
        int daysInMonth,
        int daysWithData,
        CardCommission card,
        List<FuelProfit> fuels,
        FuelProfitTotals totals) {
}
