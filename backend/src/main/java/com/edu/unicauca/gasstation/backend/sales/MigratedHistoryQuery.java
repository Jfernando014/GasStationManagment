package com.edu.unicauca.gasstation.backend.sales;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Public read contract for the historical days migrated from the August 2026 operational sheets.
 *
 * <p>It is intentionally separate from {@link SellerCloseQuery}: the latter publishes certified
 * closes stored in the database, while this query only exposes the migrated historical snapshot.
 * Consumers must prefer {@link SellerCloseQuery} and use this query as a fallback.
 *
 * @author javiersolanop777
 */
public interface MigratedHistoryQuery {

    /**
     * Returns the migrated snapshot of a date, or empty when the date was not migrated.
     */
    Optional<MigratedDay> findDay(LocalDate date);

    /**
     * Migrated snapshot of one operational day.
     */
    record MigratedDay(
            LocalDate date,
            List<VendorSale> vendors,
            ElectronicSummary electronic,
            List<InventoryVariation> variations,
            CashSummary cash
    ) {
        public MigratedDay {
            vendors = vendors != null ? List.copyOf(vendors) : List.of();
            variations = variations != null ? List.copyOf(variations) : List.of();
        }
    }

    /**
     * Vendor sale line of the migrated day.
     */
    record VendorSale(
            String vendorDocument,
            String vendorName,
            String shiftName,
            BigDecimal totalGallons,
            BigDecimal basketAmount,
            BigDecimal totalSales,
            BigDecimal totalDelivered,
            BigDecimal discrepancy,
            String resultStatus
    ) {}

    /**
     * Electronic payments summary of the migrated day.
     */
    record ElectronicSummary(
            BigDecimal mastercard,
            BigDecimal visa,
            BigDecimal amex,
            BigDecimal qr,
            BigDecimal points,
            BigDecimal totalEds
    ) {}

    /**
     * Fuel variation of the migrated day.
     */
    record InventoryVariation(
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
     * Cash and Dominus summary of the migrated day.
     */
    record CashSummary(
            Long planilla,
            BigDecimal previousCash,
            BigDecimal accumulatedSavings,
            BigDecimal dominusBalance
    ) {}
}
