package com.edu.unicauca.gasstation.backend.reports.domain.models;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Normalized operational day used by the reports module. It merges the public sales, inventory
 * and cash contracts, and records whether the data came from the live database or from the
 * migrated August history.
 *
 * @author javiersolanop777
 */
public record ReportDay(
        LocalDate date,
        Source source,
        BigDecimal totalGallons,
        BigDecimal totalDelivered,
        BigDecimal totalCash,
        BigDecimal totalBasket,
        BigDecimal totalDiscrepancy,
        List<FuelSale> fuelSales,
        List<VendorRow> vendors,
        List<FuelVariation> variations,
        CashSummary cash,
        int alertCount
) {

    public enum Source { LIVE, MIGRATED }

    public ReportDay {
        fuelSales = fuelSales != null ? List.copyOf(fuelSales) : List.of();
        vendors = vendors != null ? List.copyOf(vendors) : List.of();
        variations = variations != null ? List.copyOf(variations) : List.of();
    }

    public record FuelSale(String fuelType, BigDecimal gallons) {}

    public record VendorRow(
            String vendorDocument,
            String vendorName,
            String shiftName,
            BigDecimal gallons,
            BigDecimal basketAmount,
            BigDecimal totalDelivered,
            BigDecimal dominusTirilla,
            BigDecimal discrepancy,
            String resultStatus
    ) {}

    public record FuelVariation(
            String fuelType,
            BigDecimal initialGallons,
            BigDecimal entriesGallons,
            BigDecimal soldGallons,
            BigDecimal theoreticalGallons,
            BigDecimal measuredGallons,
            BigDecimal varianceGallons,
            BigDecimal toleranceGallons,
            boolean outOfTolerance,
            String status
    ) {}

    /**
     * Cash and Dominus values of the day. Fields that a source does not provide stay null.
     */
    public record CashSummary(
            Long planilla,
            String journalStatus,
            BigDecimal cashBalance,
            BigDecimal dailyCash,
            BigDecimal planillaCashBalance,
            BigDecimal dominusAccumulatedBalance,
            BigDecimal difference,
            BigDecimal previousCash,
            BigDecimal accumulatedSavings
    ) {}
}
