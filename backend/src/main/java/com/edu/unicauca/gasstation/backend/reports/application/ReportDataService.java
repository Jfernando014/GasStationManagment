package com.edu.unicauca.gasstation.backend.reports.application;

import com.edu.unicauca.gasstation.backend.inventory.InventoryClosingQuery;
import com.edu.unicauca.gasstation.backend.inventory.InventoryClosingSnapshot;
import com.edu.unicauca.gasstation.backend.reports.domain.models.ReportDay;
import com.edu.unicauca.gasstation.backend.reports.domain.models.ReportDay.CashSummary;
import com.edu.unicauca.gasstation.backend.reports.domain.models.ReportDay.FuelSale;
import com.edu.unicauca.gasstation.backend.reports.domain.models.ReportDay.FuelVariation;
import com.edu.unicauca.gasstation.backend.reports.domain.models.ReportDay.Source;
import com.edu.unicauca.gasstation.backend.reports.domain.models.ReportDay.VendorRow;
import com.edu.unicauca.gasstation.backend.sales.CashBalanceQuery;
import com.edu.unicauca.gasstation.backend.sales.CashBalanceTableInfo;
import com.edu.unicauca.gasstation.backend.sales.MigratedHistoryQuery;
import com.edu.unicauca.gasstation.backend.sales.MigratedHistoryQuery.MigratedDay;
import com.edu.unicauca.gasstation.backend.sales.MigratedHistoryQuery.VendorSale;
import com.edu.unicauca.gasstation.backend.sales.SellerCloseQuery;
import com.edu.unicauca.gasstation.backend.sales.SellerCloseQuery.DaySummary;
import com.edu.unicauca.gasstation.backend.sales.SellerCloseQuery.VendorCloseSummary;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Merges the public sales, inventory and cash contracts into normalized report days.
 *
 * <p>Live data is always preferred. The migrated August history is used only when the live
 * sources do not publish anything for the date.
 *
 * @author javiersolanop777
 */
@Service
@Transactional(readOnly = true)
public class ReportDataService {

    private final SellerCloseQuery sellerCloseQuery;
    private final InventoryClosingQuery inventoryClosingQuery;
    private final CashBalanceQuery cashBalanceQuery;
    private final MigratedHistoryQuery migratedHistoryQuery;

    public ReportDataService(SellerCloseQuery sellerCloseQuery,
                             InventoryClosingQuery inventoryClosingQuery,
                             CashBalanceQuery cashBalanceQuery,
                             MigratedHistoryQuery migratedHistoryQuery) {
        this.sellerCloseQuery = sellerCloseQuery;
        this.inventoryClosingQuery = inventoryClosingQuery;
        this.cashBalanceQuery = cashBalanceQuery;
        this.migratedHistoryQuery = migratedHistoryQuery;
    }

    public List<ReportDay> findDays(LocalDate from, LocalDate to) {
        List<ReportDay> days = new ArrayList<>();
        for (LocalDate date = from; !date.isAfter(to); date = date.plusDays(1)) {
            findDay(date).ifPresent(days::add);
        }
        return days;
    }

    public Optional<ReportDay> findDay(LocalDate date) {
        Optional<DaySummary> liveDay = sellerCloseQuery.findDaySummary(date);
        List<VendorCloseSummary> liveVendors = sellerCloseQuery.findVendorClosesByDate(date);
        InventoryClosingSnapshot inventory = inventoryClosingQuery.findClosingSnapshot(date);
        Optional<CashBalanceTableInfo> liveCash = cashBalanceQuery.findDailyCashBalance(date);
        Optional<MigratedDay> migrated = migratedHistoryQuery.findDay(date);

        boolean hasLive = liveDay.isPresent()
                || !liveVendors.isEmpty()
                || inventory.hasMeasurement()
                || !inventory.variations().isEmpty()
                || liveCash.isPresent();
        if (hasLive) {
            return Optional.of(buildLive(date, liveDay.orElse(null), liveVendors, inventory,
                    liveCash.orElse(null), migrated.orElse(null)));
        }

        return migrated.map(this::buildMigrated);
    }

    private ReportDay buildLive(LocalDate date, DaySummary day, List<VendorCloseSummary> vendors,
                                InventoryClosingSnapshot inventory, CashBalanceTableInfo cash,
                                MigratedDay migrated) {
        List<FuelSale> fuelSales = day == null ? List.of() : List.of(
                new FuelSale("MOTOR", day.gallonsMotor()),
                new FuelSale("DIESEL", day.gallonsDiesel()),
                new FuelSale("EXTRA", day.gallonsExtra()),
                new FuelSale("MAXPRO", day.gallonsMaxpro())
        );
        List<VendorRow> vendorRows = vendors.stream().map(this::toVendorRow).toList();
        List<FuelVariation> variations = !inventory.variations().isEmpty()
                ? inventory.variations().stream().map(this::toVariation).toList()
                : migrated != null ? migrated.variations().stream().map(this::toVariation).toList() : List.of();
        if (fuelSales.isEmpty() && !variations.isEmpty()) {
            fuelSales = variations.stream()
                    .map(variation -> new FuelSale(variation.fuelType(), variation.soldGallons()))
                    .toList();
        }

        BigDecimal totalGallons = day != null ? day.totalFuelGallons() : sum(vendorRows.stream().map(VendorRow::gallons).toList());
        BigDecimal totalDelivered = day != null ? day.totalDelivered() : sum(vendorRows.stream().map(VendorRow::totalDelivered).toList());
        BigDecimal totalCash = day != null ? day.totalCash() : null;
        BigDecimal totalBasket = day != null ? day.basketDominusAmount() : sum(vendorRows.stream().map(VendorRow::basketAmount).toList());
        BigDecimal totalDiscrepancy = day != null ? day.totalDiscrepancy() : sum(vendorRows.stream().map(VendorRow::discrepancy).toList());
        CashSummary cashSummary = cash != null ? toCash(cash) : migrated != null ? toCash(migrated) : null;

        return new ReportDay(date, Source.LIVE, totalGallons, totalDelivered, totalCash, totalBasket,
                totalDiscrepancy, fuelSales, vendorRows, variations, cashSummary, inventory.alerts().size());
    }

    private ReportDay buildMigrated(MigratedDay migrated) {
        List<VendorRow> vendors = migrated.vendors().stream().map(this::toVendorRow).toList();
        List<FuelVariation> variations = migrated.variations().stream().map(this::toVariation).toList();
        List<FuelSale> fuelSales = variations.stream()
                .map(variation -> new FuelSale(variation.fuelType(), variation.soldGallons()))
                .toList();
        BigDecimal totalGallons = sum(vendors.stream().map(VendorRow::gallons).toList());
        BigDecimal totalDelivered = sum(vendors.stream().map(VendorRow::totalDelivered).toList());
        BigDecimal totalBasket = sum(vendors.stream().map(VendorRow::basketAmount).toList());
        BigDecimal totalDiscrepancy = sum(vendors.stream().map(VendorRow::discrepancy).toList());
        return new ReportDay(migrated.date(), Source.MIGRATED, totalGallons, totalDelivered, null, totalBasket,
                totalDiscrepancy, fuelSales, vendors, variations, toCash(migrated), 0);
    }

    private VendorRow toVendorRow(VendorCloseSummary vendor) {
        return new VendorRow(vendor.vendorDocument(), vendor.vendorName(), vendor.shiftName(),
                vendorGallons(vendor), vendor.basketDominusAmount(), vendor.totalDelivered(),
                vendor.dominusTirilla(), vendor.realDiscrepancy(), vendor.resultStatus());
    }

    private VendorRow toVendorRow(VendorSale vendor) {
        BigDecimal tirilla = vendor.totalDelivered() == null
                ? null
                : vendor.totalDelivered().subtract(zero(vendor.discrepancy()));
        return new VendorRow(vendor.vendorDocument(), vendor.vendorName(), vendor.shiftName(),
                vendor.totalGallons(), vendor.basketAmount(), vendor.totalDelivered(), tirilla,
                vendor.discrepancy(), vendor.resultStatus());
    }

    private FuelVariation toVariation(InventoryClosingSnapshot.FuelVariationData variation) {
        return new FuelVariation(variation.fuelType(), variation.initialGallons(), variation.entriesGallons(),
                variation.soldGallons(), variation.theoreticalGallons(), variation.measuredGallons(),
                variation.varianceGallons(), variation.toleranceGallons(), variation.outOfTolerance(),
                variation.status());
    }

    private FuelVariation toVariation(MigratedHistoryQuery.InventoryVariation variation) {
        return new FuelVariation(variation.fuelType(), variation.initialGallons(), variation.entriesGallons(),
                variation.soldGallons(), variation.theoreticalGallons(), variation.measuredGallons(),
                variation.varianceGallons(), variation.toleranceGallons(), variation.outOfTolerance(),
                variation.status());
    }

    private CashSummary toCash(CashBalanceTableInfo cash) {
        if (cash == null) {
            return null;
        }
        return new CashSummary(cash.journalConsecutive(), cash.journalStatus(), cash.cashBalance(),
                cash.dailyCash(), cash.planillaCashBalance(), cash.dominusAccumulatedBalance(), cash.difference(),
                cash.savingsFund().previousBalance(), cash.savingsFund().accumulatedBalance());
    }

    private CashSummary toCash(MigratedDay migrated) {
        if (migrated.cash() == null) {
            return null;
        }
        return new CashSummary(migrated.cash().planilla(), "MIGRATED", null, null, null,
                migrated.cash().dominusBalance(), null, migrated.cash().previousCash(),
                migrated.cash().accumulatedSavings());
    }

    private static BigDecimal vendorGallons(VendorCloseSummary vendor) {
        return sum(List.of(vendor.gallonsMotor(), vendor.gallonsDiesel(), vendor.gallonsExtra(), vendor.gallonsMaxpro()));
    }

    private static BigDecimal sum(List<BigDecimal> values) {
        BigDecimal total = BigDecimal.ZERO;
        for (BigDecimal value : values) {
            total = total.add(zero(value));
        }
        return total;
    }

    private static BigDecimal zero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
