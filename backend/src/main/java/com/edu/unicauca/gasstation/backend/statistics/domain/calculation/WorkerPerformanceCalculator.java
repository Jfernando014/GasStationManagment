package com.edu.unicauca.gasstation.backend.statistics.domain.calculation;

import com.edu.unicauca.gasstation.backend.statistics.domain.calculation.SellerPerformance.Measure;
import com.edu.unicauca.gasstation.backend.statistics.domain.calculation.WorkerPerformance.ClosedShiftsCount;
import com.edu.unicauca.gasstation.backend.statistics.domain.calculation.WorkerPerformance.MaxSale;
import com.edu.unicauca.gasstation.backend.statistics.domain.calculation.WorkerPerformance.PerformanceWeekday;
import com.edu.unicauca.gasstation.backend.statistics.domain.calculation.WorkerPerformance.Report;
import com.edu.unicauca.gasstation.backend.statistics.domain.calculation.WorkerPerformance.RolePerformance;
import com.edu.unicauca.gasstation.backend.statistics.domain.calculation.WorkerPerformance.WeekdayPerformance;
import com.edu.unicauca.gasstation.backend.statistics.domain.models.ShiftSaleHistory;
import com.edu.unicauca.gasstation.backend.statistics.domain.models.ShiftSaleHistory.EffectiveRole;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * Pure calculation of individual worker historical performance over confirmed shift closes.
 * Station baseline cohorts group all confirmed shifts matching weekday bucket (Monday-Sunday or HOLIDAY),
 * effective role (TITULAR / APOYO), and worked shift code, avoiding cross-shift duration bias.
 */
public final class WorkerPerformanceCalculator {

    private record RoleCode(EffectiveRole role, String workedCode) { }

    private record CohortKey(PerformanceWeekday weekday, EffectiveRole role, String workedCode) { }

    private record BucketKey(PerformanceWeekday weekday, EffectiveRole role) { }

    private static final class CohortAccumulator {
        private BigDecimal totalGallons = BigDecimal.ZERO;
        private long count;

        void add(BigDecimal gallons) {
            totalGallons = totalGallons.add(gallons);
            count++;
        }
    }

    private static final class CellAccumulator {
        private BigDecimal totalGallons = BigDecimal.ZERO;
        private int count;
        private final Map<String, Integer> countByCode = new HashMap<>();

        void add(String workedCode, BigDecimal gallons) {
            totalGallons = totalGallons.add(gallons);
            count++;
            countByCode.merge(workedCode, 1, Integer::sum);
        }

        RolePerformance toRolePerformance(PerformanceWeekday weekday, EffectiveRole role,
                                          Map<CohortKey, CohortAccumulator> stationCohorts) {
            if (count == 0) {
                return RolePerformance.empty();
            }
            BigDecimal average = totalGallons.divide(
                    BigDecimal.valueOf(count),
                    WorkerPerformance.DISPLAY_SCALE,
                    WorkerPerformance.ROUNDING_MODE);
            BigDecimal percent = computePerformancePercent(weekday, role, stationCohorts);
            return new RolePerformance(count, average, percent);
        }

        private BigDecimal computePerformancePercent(PerformanceWeekday weekday, EffectiveRole role,
                                                     Map<CohortKey, CohortAccumulator> stationCohorts) {
            // Exact rational cross-product keeps cohort averages unrounded until the final division.
            BigInteger totalDenom = BigInteger.ONE;
            for (String code : countByCode.keySet()) {
                CohortAccumulator cohort = stationCohorts.get(new CohortKey(weekday, role, code));
                totalDenom = totalDenom.multiply(BigInteger.valueOf(cohort.count));
            }
            BigDecimal scaledExpected = BigDecimal.ZERO;
            for (var entry : countByCode.entrySet()) {
                CohortAccumulator cohort = stationCohorts.get(new CohortKey(weekday, role, entry.getKey()));
                BigInteger otherDenoms = totalDenom.divide(BigInteger.valueOf(cohort.count));
                BigDecimal term = cohort.totalGallons
                        .multiply(BigDecimal.valueOf(entry.getValue()))
                        .multiply(new BigDecimal(otherDenoms));
                scaledExpected = scaledExpected.add(term);
            }
            if (scaledExpected.signum() <= 0) {
                return null;
            }
            BigDecimal scaledActual = totalGallons
                    .multiply(BigDecimal.valueOf(100))
                    .multiply(new BigDecimal(totalDenom));
            return scaledActual.divide(
                    scaledExpected,
                    WorkerPerformance.DISPLAY_SCALE,
                    WorkerPerformance.ROUNDING_MODE);
        }
    }

    public Report calculate(UUID workerId, List<ShiftSaleHistory> sales, Predicate<LocalDate> isHoliday) {
        WorkerPerformance.validateWorker(workerId);
        List<ShiftSaleHistory> confirmedSales = List.copyOf(Objects.requireNonNull(sales, "sales"));
        Objects.requireNonNull(isHoliday, "isHoliday");

        List<ShiftSaleHistory> workerSales = confirmedSales.stream()
                .filter(sale -> sale.workerId().equals(workerId))
                .toList();
        if (workerSales.isEmpty()) {
            return Report.empty();
        }

        Map<LocalDate, PerformanceWeekday> weekdayCache = new HashMap<>();
        Set<RoleCode> relevantRoleCodes = new HashSet<>();
        for (ShiftSaleHistory sale : workerSales) {
            relevantRoleCodes.add(new RoleCode(sale.effectiveRole(), sale.workedCode()));
        }

        Map<CohortKey, CohortAccumulator> stationCohorts = new HashMap<>();
        for (ShiftSaleHistory sale : confirmedSales) {
            if (!relevantRoleCodes.contains(new RoleCode(sale.effectiveRole(), sale.workedCode()))) {
                continue;
            }
            PerformanceWeekday weekday = bucketOf(sale.journal().journalDate(), weekdayCache, isHoliday);
            CohortKey key = new CohortKey(weekday, sale.effectiveRole(), sale.workedCode());
            stationCohorts.computeIfAbsent(key, ignored -> new CohortAccumulator())
                    .add(Measure.FUEL_GALLONS.value(sale));
        }

        LocalDate periodFrom = null;
        LocalDate periodTo = null;
        int asHolderCount = 0;
        int asSupportCount = 0;
        ShiftSaleHistory bestSale = null;
        BigDecimal bestGallons = null;
        Map<BucketKey, CellAccumulator> workerCells = new HashMap<>();

        for (ShiftSaleHistory sale : workerSales) {
            LocalDate date = sale.journal().journalDate();
            BigDecimal gallons = Measure.FUEL_GALLONS.value(sale);
            PerformanceWeekday weekday = bucketOf(date, weekdayCache, isHoliday);

            if (periodFrom == null || date.isBefore(periodFrom)) {
                periodFrom = date;
            }
            if (periodTo == null || date.isAfter(periodTo)) {
                periodTo = date;
            }
            if (sale.effectiveRole() == EffectiveRole.TITULAR) {
                asHolderCount++;
            } else {
                asSupportCount++;
            }
            if (isBetterSale(sale, gallons, bestSale, bestGallons)) {
                bestSale = sale;
                bestGallons = gallons;
            }

            workerCells.computeIfAbsent(new BucketKey(weekday, sale.effectiveRole()), ignored -> new CellAccumulator())
                    .add(sale.workedCode(), gallons);
        }

        Map<PerformanceWeekday, RolePerformance[]> rowCells = new EnumMap<>(PerformanceWeekday.class);
        for (PerformanceWeekday day : PerformanceWeekday.values()) {
            CellAccumulator holderAcc = workerCells.get(new BucketKey(day, EffectiveRole.TITULAR));
            CellAccumulator supportAcc = workerCells.get(new BucketKey(day, EffectiveRole.APOYO));
            rowCells.put(day, new RolePerformance[] {
                    holderAcc == null ? RolePerformance.empty() : holderAcc.toRolePerformance(day, EffectiveRole.TITULAR, stationCohorts),
                    supportAcc == null ? RolePerformance.empty() : supportAcc.toRolePerformance(day, EffectiveRole.APOYO, stationCohorts)
            });
        }

        List<WeekdayPerformance> byWeekday = Arrays.stream(PerformanceWeekday.values())
                .map(day -> {
                    RolePerformance[] roles = rowCells.get(day);
                    return new WeekdayPerformance(day, roles[0], roles[1]);
                })
                .toList();

        MaxSale maxSale = new MaxSale(
                bestGallons.setScale(WorkerPerformance.DISPLAY_SCALE, WorkerPerformance.ROUNDING_MODE),
                bestSale.journal().journalDate(),
                bestSale.workedCode());

        return new Report(
                periodFrom,
                periodTo,
                new ClosedShiftsCount(workerSales.size(), asHolderCount, asSupportCount),
                byWeekday,
                maxSale);
    }

    private static PerformanceWeekday bucketOf(LocalDate date,
                                               Map<LocalDate, PerformanceWeekday> cache,
                                               Predicate<LocalDate> isHoliday) {
        return cache.computeIfAbsent(date, d -> PerformanceWeekday.from(d, isHoliday.test(d)));
    }

    private static boolean isBetterSale(ShiftSaleHistory candidate, BigDecimal candidateGallons,
                                        ShiftSaleHistory currentBest, BigDecimal currentBestGallons) {
        if (currentBest == null) {
            return true;
        }
        int gallonsCompare = candidateGallons.compareTo(currentBestGallons);
        if (gallonsCompare != 0) {
            return gallonsCompare > 0;
        }
        int dateCompare = candidate.journal().journalDate().compareTo(currentBest.journal().journalDate());
        if (dateCompare != 0) {
            return dateCompare > 0;
        }
        return Comparator.<String>naturalOrder().compare(candidate.workedCode(), currentBest.workedCode()) > 0;
    }
}
