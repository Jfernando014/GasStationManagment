package com.edu.unicauca.gasstation.backend.sales.domain.reconciliation;

import java.time.LocalDate;
import java.util.List;

/**
 * Monthly table of Credibanco, QR and Puntos Colombia with the commission (C-05).
 *
 * @param month {@code yyyy-MM}
 * @param days one row per calendar day, in order, including days without data
 * @param daysWithData how many of those days have vendor rows, a Redeban import or a Puntos Colombia total
 */
public record MonthlyReconciliation(
        String month,
        LocalDate from,
        LocalDate to,
        List<MonthlyDayRow> days,
        int daysWithData,
        MonthlyTotals totals,
        MonthlyCommission commission) {
}
