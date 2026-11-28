package com.edu.unicauca.gasstation.backend.reports.domain.models;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Data of the COMPLEMENTO DOMINUS Excel file of one operational day.
 *
 * @author javiersolanop777
 */
public record DominusComplementReport(
        LocalDate date,
        ReportDay.Source source,
        ReportDay.CashSummary cash,
        List<ReportDay.VendorRow> vendors,
        BigDecimal totalGallons,
        BigDecimal totalDelivered,
        BigDecimal totalDominusTirilla,
        BigDecimal totalDiscrepancy,
        BigDecimal totalCash,
        BigDecimal totalBasket
) {

    public DominusComplementReport {
        vendors = vendors != null ? List.copyOf(vendors) : List.of();
    }
}
