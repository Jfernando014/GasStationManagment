package com.edu.unicauca.gasstation.backend.reports.domain.models;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Executive balance of a period, ready to be rendered as a PowerPoint presentation.
 *
 * @author javiersolanop777
 */
public record BalanceReport(
        LocalDate from,
        LocalDate to,
        List<ReportDay> days,
        int liveDays,
        int migratedDays,
        BigDecimal totalGallons,
        BigDecimal totalDelivered,
        BigDecimal totalCash,
        BigDecimal totalBasket,
        BigDecimal totalDiscrepancy,
        List<FuelVariationAggregate> fuelVariations,
        List<SellerAggregate> sellers,
        ReportDay.CashSummary lastCash
) {

    public BalanceReport {
        days = days != null ? List.copyOf(days) : List.of();
        fuelVariations = fuelVariations != null ? List.copyOf(fuelVariations) : List.of();
        sellers = sellers != null ? List.copyOf(sellers) : List.of();
    }

    public record FuelVariationAggregate(
            String fuelType,
            BigDecimal theoreticalGallons,
            BigDecimal measuredGallons,
            BigDecimal varianceGallons,
            BigDecimal toleranceGallons,
            boolean outOfTolerance,
            int days
    ) {}

    public record SellerAggregate(
            String vendorDocument,
            String vendorName,
            BigDecimal gallons,
            BigDecimal totalDelivered,
            BigDecimal basketAmount,
            BigDecimal discrepancy
    ) {}
}
