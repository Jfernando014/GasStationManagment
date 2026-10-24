package com.edu.unicauca.gasstation.backend.sales;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Public integration query for journal sales and vendor closing summaries.
 * Exposes certified daily totals and individual seller closes to other modules
 * (Reports, Inventory, Incentives, Statistics, and internal Sales reconciliation).
 */
public interface SellerCloseQuery {

    /**
     * Retrieves the consolidated daily sales totals for a given journal date.
     */
    Optional<DaySummary> findDaySummary(LocalDate journalDate);

    /**
     * Retrieves the list of individual vendor closes for a given journal date.
     */
    List<VendorCloseSummary> findVendorClosesByDate(LocalDate journalDate);

    /**
     * Retrieves a single vendor close for a specific vendor on a given journal date.
     */
    Optional<VendorCloseSummary> findVendorClose(LocalDate journalDate, String vendorDocument);

    /**
     * Consolidated day-level sales totals.
     */
    record DaySummary(
            LocalDate date,
            BigDecimal totalCash,
            BigDecimal totalGa,
            BigDecimal totalCredibanco,
            BigDecimal totalQr,
            BigDecimal totalRedemptions,
            BigDecimal totalDelivered,
            BigDecimal totalDominusTirilla,
            BigDecimal totalDiscrepancy,
            BigDecimal gallonsMotor,
            BigDecimal gallonsDiesel,
            BigDecimal gallonsExtra,
            BigDecimal gallonsMaxpro,
            BigDecimal totalFuelGallons,
            BigDecimal basketDominusAmount,
            int totalProductUnits,
            List<ProductSaleSummary> products
    ) {
        public DaySummary {
            products = products != null ? List.copyOf(products) : List.of();
        }
    }

    /**
     * Individual vendor close summary.
     */
    record VendorCloseSummary(
            LocalDate date,
            String vendorDocument,
            String vendorName,
            String shiftName,
            Integer pumpNumber,
            BigDecimal cash,
            BigDecimal ga,
            BigDecimal credibanco,
            BigDecimal qr,
            BigDecimal redemptions,
            BigDecimal totalDelivered,
            BigDecimal gallonsMotor,
            BigDecimal gallonsDiesel,
            BigDecimal gallonsExtra,
            BigDecimal gallonsMaxpro,
            BigDecimal basketDominusAmount,
            BigDecimal dominusTirilla,
            BigDecimal realDiscrepancy,
            String resultStatus,
            List<ProductSaleSummary> products
    ) {
        public VendorCloseSummary {
            products = products != null ? List.copyOf(products) : List.of();
        }
    }

    /**
     * Product sale summary line by SKU.
     */
    record ProductSaleSummary(
            String sku,
            String productName,
            String category,
            int unitsSold,
            BigDecimal unitPrice,
            BigDecimal totalAmount
    ) {}
}
