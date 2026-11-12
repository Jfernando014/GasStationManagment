package com.edu.unicauca.gasstation.backend.sales.domain.reconciliation;

import java.math.BigDecimal;

/**
 * Sum of the days of a month: the TOTAL row of the Credibanco report.
 *
 * @param totalEds Mastercard + Visa of the month (the base of the commission)
 * @param totalWithQr {@code totalEds + qr}
 * @param points Puntos Colombia total of the month
 */
public record MonthlyTotals(
        BigDecimal mastercard,
        BigDecimal visa,
        BigDecimal amex,
        BigDecimal totalEds,
        BigDecimal qr,
        BigDecimal totalWithQr,
        BigDecimal points) {
}
