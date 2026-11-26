package com.edu.unicauca.gasstation.backend.sales.domain.profit;

import com.edu.unicauca.gasstation.backend.inventory.FuelType;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * What the month sold.
 *
 * @param daysWithData days of the month that have sales data
 * @param days days with gallons sold of each fuel, in date order
 * @param cardSales Credibanco of the month, in pesos
 * @param totalSales every sale of the month in pesos, basket and lubricants included
 */
public record MonthlyFuelSales(
        int daysWithData,
        Map<FuelType, List<FuelDayInput>> days,
        BigDecimal cardSales,
        BigDecimal totalSales) {
}
