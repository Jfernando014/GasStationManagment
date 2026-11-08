package com.edu.unicauca.gasstation.backend.sales.domain.models;

import com.edu.unicauca.gasstation.backend.sales.domain.models.JournalHistoryModels.HistoryCashBalanceSummary;
import com.edu.unicauca.gasstation.backend.sales.domain.models.JournalHistoryModels.HistoryElectronicSummary;
import com.edu.unicauca.gasstation.backend.sales.domain.models.JournalHistoryModels.HistoryVendorSaleItem;
import com.edu.unicauca.gasstation.backend.sales.domain.repositories.InventoryClosingReadPort.FuelVariationSummary;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Provides historical migrated day records from the August 2026 operational Excel sheets
 * (days 01/08/2026 through 28/08/2026, excluding the live system demo date 05/08/2026 when not yet closed),
 * matching the prototype specification {@code historial-jornadas.js} (C-10, S3-06).
 */
public final class AugustMigratedHistoryCatalog {

    public static final LocalDate LIVE_DEMO_DATE = LocalDate.of(2026, 8, 5);
    private static final LocalDate AUG_START = LocalDate.of(2026, 8, 1);
    private static final LocalDate AUG_END_MIGRATED = LocalDate.of(2026, 8, 28);

    private AugustMigratedHistoryCatalog() {}

    public static boolean isMigratedSeedDate(LocalDate date) {
        if (date == null || date.equals(LIVE_DEMO_DATE)) {
            return false;
        }
        return !date.isBefore(AUG_START) && !date.isAfter(AUG_END_MIGRATED);
    }

    public static boolean hasSeedDiscrepancy(LocalDate date) {
        if (!isMigratedSeedDate(date)) {
            return false;
        }
        int day = date.getDayOfMonth();
        return day == 1 || day == 3 || day == 7 || day == 12 || day == 18 || day == 22 || day == 26;
    }

    public static Optional<MigratedDaySnapshot> findSnapshot(LocalDate date) {
        if (!isMigratedSeedDate(date)) {
            return Optional.empty();
        }
        int d = date.getDayOfMonth();
        boolean hasDisc = hasSeedDiscrepancy(date);
        BigDecimal carlosDiff = hasDisc ? new BigDecimal("-531.00") : BigDecimal.ZERO;
        String carlosStatus = hasDisc ? "FALTA" : "CUADRA";

        BigDecimal baseGallons = BigDecimal.valueOf(310 + (d * 7L) % 90);
        BigDecimal baseSales = BigDecimal.valueOf(5100000L + (d * 115000L) % 1200000L);

        List<HistoryVendorSaleItem> vendors = List.of(
                new HistoryVendorSaleItem(
                        "1001",
                        "Carlos M.",
                        "Día",
                        baseGallons,
                        new BigDecimal("57000.00"),
                        baseSales,
                        baseSales.add(carlosDiff),
                        carlosDiff,
                        carlosStatus
                ),
                new HistoryVendorSaleItem(
                        "1002",
                        "Mónica D.",
                        "Día",
                        baseGallons.subtract(new BigDecimal("65")),
                        new BigDecimal("28500.00"),
                        baseSales.subtract(new BigDecimal("980000.00")),
                        baseSales.subtract(new BigDecimal("980000.00")),
                        BigDecimal.ZERO,
                        "CUADRA"
                ),
                new HistoryVendorSaleItem(
                        "1003",
                        "Wiltom R.",
                        "Noche",
                        baseGallons.subtract(new BigDecimal("40")),
                        new BigDecimal("36000.00"),
                        baseSales.subtract(new BigDecimal("620000.00")),
                        baseSales.subtract(new BigDecimal("620000.00")),
                        BigDecimal.ZERO,
                        "CUADRA"
                )
        );

        BigDecimal mc = BigDecimal.valueOf(2450000L + (d * 45000L) % 600000L);
        BigDecimal visa = BigDecimal.valueOf(2890000L + (d * 52000L) % 700000L);
        BigDecimal qr = BigDecimal.valueOf(740000L + (d * 19000L) % 250000L);
        HistoryElectronicSummary electronic = new HistoryElectronicSummary(
                mc,
                visa,
                BigDecimal.ZERO,
                qr,
                new BigDecimal("35000.00"),
                mc.add(visa)
        );

        List<FuelVariationSummary> variations = List.of(
                new FuelVariationSummary(
                        "MOTOR",
                        new BigDecimal("4500.00"),
                        BigDecimal.ZERO,
                        new BigDecimal("480.00"),
                        new BigDecimal("4020.00"),
                        new BigDecimal("4032.50"),
                        new BigDecimal("12.50"),
                        new BigDecimal("65.00"),
                        false,
                        "OK"
                ),
                new FuelVariationSummary(
                        "DIESEL",
                        new BigDecimal("3800.00"),
                        BigDecimal.ZERO,
                        new BigDecimal("315.00"),
                        new BigDecimal("3485.00"),
                        new BigDecimal("3479.20"),
                        new BigDecimal("-5.80"),
                        new BigDecimal("55.00"),
                        false,
                        "OK"
                )
        );

        long planilla = 288L + d;
        BigDecimal accumulatedSavings = BigDecimal.valueOf((11L + d) * 250000L);
        BigDecimal previousCash = BigDecimal.valueOf(6800000L + (d * 110000L) % 900000L);
        BigDecimal dominusBalance = previousCash.add( new BigDecimal("7420000.00"));
        HistoryCashBalanceSummary cashBalance = new HistoryCashBalanceSummary(
                planilla,
                previousCash,
                accumulatedSavings,
                dominusBalance
        );

        return Optional.of(new MigratedDaySnapshot(hasDisc, vendors, electronic, variations, cashBalance));
    }

    public record MigratedDaySnapshot(
            boolean hasDiscrepancy,
            List<HistoryVendorSaleItem> vendorSales,
            HistoryElectronicSummary electronicSummary,
            List<FuelVariationSummary> variations,
            HistoryCashBalanceSummary cashBalance
    ) {}
}
