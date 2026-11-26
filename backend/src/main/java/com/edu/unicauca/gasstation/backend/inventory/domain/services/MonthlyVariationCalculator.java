package com.edu.unicauca.gasstation.backend.inventory.domain.services;

import com.edu.unicauca.gasstation.backend.inventory.FuelType;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.AlertLevel;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.DailyVariationEntry;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.FuelMonthlyVariation;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.FuelVariation;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.FuelVariationSummary;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.GallonRange;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.MonthlyVariationTotals;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.ShortageRangeCount;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.SurplusRangeCount;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.VariationAlertSetting;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.VariationStatus;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Monthly control of a fuel, built from its daily variations. Pure Java: no Spring, no database.
 *
 * <ul>
 *   <li>Only the days with status OK add to the cumulative figures and to the ranges.</li>
 *   <li>A day that cannot be calculated tells nothing: it neither breaks nor advances a streak of alerts, so
 *       "the previous day" is the previous calculated day of the month.</li>
 *   <li>A figure without data is null, never zero.</li>
 * </ul>
 */
final class MonthlyVariationCalculator {

    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private static final int PERCENTAGE_SCALE = 4;
    private static final int GALLONS_SCALE = 3;

    private MonthlyVariationCalculator() {
    }

    /**
     * @param days every calendar day of the month, in order
     * @param variations daily variation of the fuel for each of those days, in the same order; an element is null
     *                   when the fuel has no daily variation (it has no tanks)
     * @param target sales target of the month in gallons, or null
     */
    static FuelMonthlyVariation calculate(FuelType fuelType, List<LocalDate> days, List<FuelVariation> variations,
                                          VariationAlertSetting setting, BigDecimal target) {
        List<DailyVariationEntry> entries = new ArrayList<>(days.size());
        List<AlertLevel> levels = new ArrayList<>();
        BigDecimal cumulativeSold = BigDecimal.ZERO;
        BigDecimal cumulativeVariance = BigDecimal.ZERO;
        for (int i = 0; i < days.size(); i++) {
            FuelVariation variation = variations.get(i);
            if (variation == null) {
                entries.add(DailyVariationEntry.builder()
                        .date(days.get(i))
                        .status(VariationStatus.INCOMPLETE_MEASUREMENT)
                        .build());
                continue;
            }
            DailyVariationEntry.DailyVariationEntryBuilder entry = entryOf(days.get(i), variation);
            if (variation.getStatus() == VariationStatus.OK) {
                cumulativeSold = cumulativeSold.add(variation.getSoldGallons());
                cumulativeVariance = cumulativeVariance.add(variation.getVarianceGallons());
                BigDecimal percentage = percentage(cumulativeVariance, cumulativeSold);
                AlertLevel level = null;
                if (percentage != null) {
                    level = nextLevel(cumulativeVariance, cumulativeSold, levels, setting);
                    levels.add(level);
                }
                entry.cumulativeSoldGallons(cumulativeSold)
                        .cumulativeVarianceGallons(cumulativeVariance)
                        .cumulativePercentage(percentage)
                        .cumulativeStatus(level);
            }
            entries.add(entry.build());
        }

        List<DailyVariationEntry> calculated = entries.stream()
                .filter(e -> e.getStatus() == VariationStatus.OK)
                .toList();
        MonthlyVariationTotals totals = totals(entries, calculated);
        return FuelMonthlyVariation.builder()
                .fuelType(fuelType)
                .days(entries)
                .totals(totals)
                .targetGallons(target)
                .remainingGallons(target == null || totals.getSoldGallons() == null
                        ? null : target.subtract(totals.getSoldGallons()))
                .shortageRanges(shortageRanges(calculated, setting))
                .surplusRanges(surplusRanges(calculated))
                .summary(summary(calculated))
                .build();
    }

    private static DailyVariationEntry.DailyVariationEntryBuilder entryOf(LocalDate date, FuelVariation variation) {
        boolean calculated = variation.getStatus() == VariationStatus.OK;
        // Without a calculation, "no entries" cannot be told apart from "nothing was recorded"
        boolean knownEntries = calculated || variation.getEntriesGallons().signum() != 0;
        return DailyVariationEntry.builder()
                .date(date)
                .status(variation.getStatus())
                .initialGallons(variation.getInitialGallons())
                .entriesGallons(knownEntries ? variation.getEntriesGallons() : null)
                .soldGallons(variation.getSoldGallons())
                .theoreticalGallons(variation.getTheoreticalGallons())
                .measuredGallons(variation.getMeasuredGallons())
                .varianceGallons(variation.getVarianceGallons())
                .variancePercentage(calculated
                        ? percentage(variation.getVarianceGallons(), variation.getSoldGallons()) : null)
                .toleranceGallons(variation.getToleranceGallons())
                .outOfTolerance(calculated ? variation.isOutOfTolerance() : null);
    }

    /**
     * Within the limit there is no alert. Beyond it: ALARMA if the previous day was already in ALARMA or the last N
     * were in AYUDA; otherwise AYUDA if the previous day was in AYUDA or the last N were in REVISAR; otherwise
     * REVISAR. The comparison uses the exact percentage, not the rounded one.
     */
    private static AlertLevel nextLevel(BigDecimal variance, BigDecimal sold, List<AlertLevel> previous,
                                        VariationAlertSetting setting) {
        if (variance.abs().multiply(HUNDRED)
                .compareTo(setting.getCumulativeLimitPercentage().multiply(sold.abs())) <= 0) {
            return AlertLevel.OK;
        }
        AlertLevel last = previous.isEmpty() ? null : previous.get(previous.size() - 1);
        int days = setting.getEscalationDays();
        if (last == AlertLevel.ALARMA || lastDaysWere(previous, days, AlertLevel.AYUDA)) {
            return AlertLevel.ALARMA;
        }
        if (last == AlertLevel.AYUDA || lastDaysWere(previous, days, AlertLevel.REVISAR)) {
            return AlertLevel.AYUDA;
        }
        return AlertLevel.REVISAR;
    }

    private static boolean lastDaysWere(List<AlertLevel> previous, int days, AlertLevel level) {
        return previous.size() >= days
                && previous.subList(previous.size() - days, previous.size()).stream().allMatch(l -> l == level);
    }

    private static MonthlyVariationTotals totals(List<DailyVariationEntry> entries,
                                                 List<DailyVariationEntry> calculated) {
        BigDecimal variance = sum(calculated.stream().map(DailyVariationEntry::getVarianceGallons).toList());
        BigDecimal calculatedSold = sum(calculated.stream().map(DailyVariationEntry::getSoldGallons).toList());
        List<BigDecimal> sellingDays = entries.stream()
                .map(DailyVariationEntry::getSoldGallons)
                .filter(sold -> sold != null && sold.signum() != 0)
                .toList();
        DailyVariationEntry lastMeasured = entries.stream()
                .filter(e -> e.getMeasuredGallons() != null)
                .reduce((first, second) -> second)
                .orElse(null);
        return MonthlyVariationTotals.builder()
                .entriesGallons(sum(entries.stream().map(DailyVariationEntry::getEntriesGallons).toList()))
                .soldGallons(sum(entries.stream().map(DailyVariationEntry::getSoldGallons).toList()))
                .varianceGallons(variance)
                .variancePercentage(percentage(variance, calculatedSold))
                .averageDailySoldGallons(sellingDays.isEmpty() ? null : sum(sellingDays)
                        .divide(BigDecimal.valueOf(sellingDays.size()), GALLONS_SCALE, RoundingMode.HALF_UP))
                .lastMeasuredGallons(lastMeasured == null ? null : lastMeasured.getMeasuredGallons())
                .lastMeasuredDate(lastMeasured == null ? null : lastMeasured.getDate())
                .build();
    }

    /** OK below the limit, REVISAR on it and ALARMA above it. */
    private static List<ShortageRangeCount> shortageRanges(List<DailyVariationEntry> calculated,
                                                           VariationAlertSetting setting) {
        Map<GallonRange, Integer> counts = countByRange(calculated, -1);
        return Arrays.stream(GallonRange.values()).map(range -> {
            int count = counts.getOrDefault(range, 0);
            int limit = setting.shortageLimitOf(range);
            AlertLevel status = null;
            if (!calculated.isEmpty()) {
                status = count < limit ? AlertLevel.OK : count == limit ? AlertLevel.REVISAR : AlertLevel.ALARMA;
            }
            return new ShortageRangeCount(range, count, limit, status);
        }).toList();
    }

    private static List<SurplusRangeCount> surplusRanges(List<DailyVariationEntry> calculated) {
        Map<GallonRange, Integer> counts = countByRange(calculated, 1);
        return Arrays.stream(GallonRange.values())
                .map(range -> new SurplusRangeCount(range, counts.getOrDefault(range, 0)))
                .toList();
    }

    /**
     * @param sign -1 to count the shortages, 1 to count the surpluses
     */
    private static Map<GallonRange, Integer> countByRange(List<DailyVariationEntry> calculated, int sign) {
        Map<GallonRange, Integer> counts = new EnumMap<>(GallonRange.class);
        calculated.stream()
                .map(DailyVariationEntry::getVarianceGallons)
                .filter(variance -> variance.signum() == sign)
                .forEach(variance -> GallonRange.of(variance.abs())
                        .ifPresent(range -> counts.merge(range, 1, Integer::sum)));
        return counts;
    }

    private static FuelVariationSummary summary(List<DailyVariationEntry> calculated) {
        DailyVariationEntry last = calculated.isEmpty() ? null : calculated.get(calculated.size() - 1);
        return FuelVariationSummary.builder()
                .cumulativeStatus(last == null ? null : last.getCumulativeStatus())
                .lastCalculatedDate(last == null ? null : last.getDate())
                .calculatedDays(calculated.size())
                .outOfToleranceDays((int) calculated.stream()
                        .filter(e -> Boolean.TRUE.equals(e.getOutOfTolerance()))
                        .count())
                .build();
    }

    /** Sum of the known values; null when there is none. */
    private static BigDecimal sum(List<BigDecimal> values) {
        return values.stream().filter(v -> v != null).reduce(BigDecimal::add).orElse(null);
    }

    private static BigDecimal percentage(BigDecimal part, BigDecimal whole) {
        if (part == null || whole == null || whole.signum() == 0) {
            return null;
        }
        return part.multiply(HUNDRED).divide(whole, PERCENTAGE_SCALE, RoundingMode.HALF_UP);
    }
}
