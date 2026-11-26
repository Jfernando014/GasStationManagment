package com.edu.unicauca.gasstation.backend.sales.domain.profit;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Figures behind the card commission. Percentages go from 0 to 100; the commission rate is a fraction.
 *
 * @param appliedPercentage the manual or the real one, according to the mode
 * @param realPercentage suggestion calculated from the month; null without sales in pesos
 * @param cardSales Credibanco of the month
 * @param totalSales Dominus tirilla of the month
 * @param commissionRateDate date on which the rate was looked up
 */
public record CardCommission(
        CardPercentageMode mode,
        BigDecimal appliedPercentage,
        BigDecimal manualPercentage,
        BigDecimal realPercentage,
        BigDecimal cardSales,
        BigDecimal totalSales,
        String realPercentageNote,
        BigDecimal commissionRate,
        LocalDate commissionRateDate) {
}
