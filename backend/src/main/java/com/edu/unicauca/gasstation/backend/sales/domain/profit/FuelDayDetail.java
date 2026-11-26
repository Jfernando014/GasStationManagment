package com.edu.unicauca.gasstation.backend.sales.domain.profit;

import java.math.BigDecimal;
import java.time.LocalDate;

/** One day of a fuel: what was used and what came out. Null means that it cannot be calculated. */
public record FuelDayDetail(
        LocalDate date,
        BigDecimal gallons,
        BigDecimal salePrice,
        BigDecimal purchaseCost,
        BigDecimal freightPerGallon,
        BigDecimal financialCostPerGallon,
        BigDecimal marginPerGallon,
        BigDecimal sales,
        BigDecimal profit) {
}
