package com.edu.unicauca.gasstation.backend.sales.domain.models;

import com.edu.unicauca.gasstation.backend.sales.domain.repositories.InventoryClosingReadPort.FuelVariationSummary;
import com.edu.unicauca.gasstation.backend.sales.domain.repositories.InventoryClosingReadPort.InventoryAlertSummary;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Domain models for the monthly journal history calendar and migrated day detail (C-10, S3-06).
 */
public final class JournalHistoryModels {

    private JournalHistoryModels() {}

    public record JournalHistoryCalendar(
            String yearMonth,
            int closedInSystemCount,
            int migratedDaysCount,
            int inProgressCount,
            int daysWithIssuesCount,
            List<JournalCalendarDay> days
    ) {}

    public record JournalCalendarDay(
            LocalDate date,
            int dayOfMonth,
            String type,
            String status,
            Long journalId,
            Long consecutive,
            boolean hasDiscrepancy,
            boolean hasAlert,
            List<String> markers
    ) {}

    public record JournalHistoryDayDetail(
            LocalDate date,
            String type,
            String status,
            Long journalId,
            Long consecutive,
            LocalDateTime closedAt,
            boolean hasDiscrepancy,
            boolean hasAlert,
            List<String> markers,
            List<HistoryVendorSaleItem> vendorSales,
            HistoryElectronicSummary electronicSummary,
            List<FuelVariationSummary> variations,
            HistoryCashBalanceSummary cashBalance,
            List<InventoryAlertSummary> alerts
    ) {}

    public record HistoryVendorSaleItem(
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

    public record HistoryElectronicSummary(
            BigDecimal mastercard,
            BigDecimal visa,
            BigDecimal amex,
            BigDecimal qr,
            BigDecimal points,
            BigDecimal totalEds
    ) {}

    public record HistoryCashBalanceSummary(
            Long planilla,
            BigDecimal previousCash,
            BigDecimal accumulatedSavings,
            BigDecimal dominusBalance
    ) {}
}
