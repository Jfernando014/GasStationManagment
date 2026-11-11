package com.edu.unicauca.gasstation.backend.statistics.domain.calculation;

import com.edu.unicauca.gasstation.backend.statistics.domain.models.ShiftSaleHistory;
import com.edu.unicauca.gasstation.backend.statistics.domain.models.ShiftSaleHistoryRange;
import com.edu.unicauca.gasstation.backend.statistics.domain.models.ShiftSaleIdentity;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.edu.unicauca.gasstation.backend.statistics.domain.calculation.SellerPerformance.*;
import static com.edu.unicauca.gasstation.backend.statistics.domain.calculation.SellerPerformance.Availability.*;

/** Pure calculations over confirmed history. Missing rules/data are never substituted by sales or targets. */
public final class SellerPerformanceCalculator {
    public static final String BASELINE =
            "ALL_CONFIRMED_EQUIVALENT_SELLER_SHIFTS_IN_REQUESTED_WINDOW_INCLUDING_SELLER";

    public ComplianceReport compliance(ShiftSaleHistoryRange history, Measure measure,
                                       List<IndividualTarget> targets, int scale) {
        validate(history, measure, scale);
        targets = List.copyOf(targets);
        var window = window(history);
        var reason = historyReason(history, measure);
        if (reason != null) {
            return new ComplianceReport(window, measure, reason == Reason.NO_CONFIRMED_SELLER_CLOSES ? NO_DATA : UNAVAILABLE,
                    reason, coverage(history), List.of());
        }
        var byWorker = unique(targets, IndividualTarget::workerId);
        var sellers = new ArrayList<Compliance>();
        var groups = history.sales().stream().collect(Collectors.groupingBy(ShiftSaleHistory::workerId));
        for (var entry : groups.entrySet()) {
            var rows = entry.getValue();
            var total = sum(rows, measure);
            var target = byWorker.get(entry.getKey());
            Reason unavailable = target == null ? Reason.TARGET_NOT_PROVIDED
                    : !target.window().equals(window) ? Reason.TARGET_PERIOD_MISMATCH
                    : target.measure() != measure ? Reason.TARGET_MEASURE_MISMATCH
                    : target.amount().signum() == 0 ? Reason.ZERO_TARGET : null;
            var ratio = unavailable == null ? new ExactRatio(total, target.amount()) : null;
            sellers.add(new Compliance(entry.getKey(), rows.getLast().vendorName(), total,
                    target == null ? null : target.amount(), unavailable == null ? AVAILABLE : UNAVAILABLE, unavailable,
                    ratio, ratio == null ? null : ratio.percent(scale), target == null ? null : target.source()));
        }
        sellers.sort((left, right) -> {
            if (left.ratio() == null && right.ratio() != null) return 1;
            if (left.ratio() != null && right.ratio() == null) return -1;
            int comparison = left.ratio() == null ? 0 : right.ratio().compareTo(left.ratio());
            return comparison == 0 ? left.workerId().toString().compareTo(right.workerId().toString()) : comparison;
        });
        var availability = availability(sellers.stream().map(Compliance::availability).toList());
        return new ComplianceReport(window, measure, availability, reportReason(availability), coverage(history), sellers);
    }

    public ComparisonReport compare(ShiftSaleHistoryRange history, Measure measure,
                                    List<ShiftClassification> classifications, int scale) {
        validate(history, measure, scale);
        classifications = List.copyOf(classifications);
        var reason = historyReason(history, measure);
        if (reason == null && !classified(history, classifications)) reason = Reason.SHIFT_CLASSIFICATION_NOT_PROVIDED;
        if (reason != null) {
            return new ComparisonReport(window(history), measure,
                    reason == Reason.NO_CONFIRMED_SELLER_CLOSES ? NO_DATA : UNAVAILABLE,
                    reason, BASELINE, coverage(history), List.of());
        }
        var periods = unique(classifications, ShiftClassification::identity);
        var groups = groups(history, periods);
        var cohorts = history.sales().stream().collect(Collectors.groupingBy(sale -> scenario(sale, periods)));
        var sellers = new ArrayList<Comparison>();
        for (var entry : groups.entrySet()) {
            var rows = entry.getValue();
            var cohort = cohorts.get(entry.getKey().scenario());
            var total = sum(rows, measure);
            var stationTotal = sum(cohort, measure);
            var ratio = stationTotal.signum() == 0 ? null : new ExactRatio(
                    total.multiply(BigDecimal.valueOf(cohort.size())),
                    stationTotal.multiply(BigDecimal.valueOf(rows.size())));
            sellers.add(new Comparison(entry.getKey().worker(), rows.getLast().vendorName(), entry.getKey().scenario(),
                    total, rows.size(), average(total, rows.size(), scale), stationTotal, cohort.size(),
                    average(stationTotal, cohort.size(), scale), ratio, ratio == null ? null : ratio.percent(scale),
                    ratio == null ? UNAVAILABLE : AVAILABLE, ratio == null ? Reason.ZERO_BASELINE : null));
        }
        var availability = availability(sellers.stream().map(Comparison::availability).toList());
        return new ComparisonReport(window(history), measure, availability, reportReason(availability),
                BASELINE, coverage(history), sellers);
    }

    public NormalizationReport normalize(ShiftSaleHistoryRange history, Measure measure,
                                         List<ShiftClassification> classifications,
                                         List<ShiftMultiplier> multipliers, int scale) {
        validate(history, measure, scale);
        classifications = List.copyOf(classifications);
        multipliers = List.copyOf(multipliers);
        var reason = historyReason(history, measure);
        if (reason == null && !classified(history, classifications)) reason = Reason.SHIFT_CLASSIFICATION_NOT_PROVIDED;
        if (reason != null) {
            return new NormalizationReport(window(history), measure,
                    reason == Reason.NO_CONFIRMED_SELLER_CLOSES ? NO_DATA : UNAVAILABLE,
                    reason, coverage(history), List.of());
        }
        var parameters = unique(multipliers, ShiftMultiplier::period);
        var groups = groups(history, unique(classifications, ShiftClassification::identity));
        var sellers = new ArrayList<NormalizedSale>();
        for (var entry : groups.entrySet()) {
            var factor = parameters.get(entry.getKey().scenario().period());
            Reason unavailable = factor == null ? Reason.MULTIPLIER_NOT_PROVIDED
                    : !factor.window().equals(window(history)) ? Reason.MULTIPLIER_PERIOD_MISMATCH
                    : factor.measure() != measure ? Reason.MULTIPLIER_MEASURE_MISMATCH : null;
            var rows = entry.getValue();
            var total = sum(rows, measure);
            sellers.add(new NormalizedSale(entry.getKey().worker(), rows.getLast().vendorName(), entry.getKey().scenario(),
                    total, rows.size(), factor == null ? null : factor.multiplier(),
                    unavailable == null ? total.multiply(factor.multiplier()) : null,
                    unavailable == null ? AVAILABLE : UNAVAILABLE, unavailable, factor == null ? null : factor.source()));
        }
        var availability = availability(sellers.stream().map(NormalizedSale::availability).toList());
        return new NormalizationReport(window(history), measure, availability, reportReason(availability),
                coverage(history), sellers);
    }

    private static void validate(ShiftSaleHistoryRange history, Measure measure, int scale) {
        Objects.requireNonNull(history); Objects.requireNonNull(measure); validateScale(scale);
    }

    private static Reason historyReason(ShiftSaleHistoryRange history, Measure measure) {
        if (!history.complete()) return Reason.HISTORY_INCOMPLETE;
        if (history.sales().isEmpty()) return Reason.NO_CONFIRMED_SELLER_CLOSES;
        if (history.sales().stream().anyMatch(measure::containsAdjustment)) return Reason.NEGATIVE_OBSERVATIONS_REQUIRE_POLICY;
        return null;
    }

    private static boolean classified(ShiftSaleHistoryRange history, List<ShiftClassification> classifications) {
        var periods = unique(classifications, ShiftClassification::identity);
        var dayTypes = new HashMap<java.time.LocalDate, DayType>();
        for (var identity : periods.keySet()) {
            var previous = dayTypes.putIfAbsent(identity.journalDate(), periods.get(identity).dayType());
            if (previous != null && previous != periods.get(identity).dayType()) {
                throw new IllegalArgumentException("Conflicting explicit day types for one operational date");
            }
            if (identity.journalDate().isBefore(history.from()) || identity.journalDate().isAfter(history.to())) {
                throw new IllegalArgumentException("Classification outside requested operational dates");
            }
        }
        return history.sales().stream().allMatch(sale -> periods.containsKey(sale.identity()));
    }

    private record WorkerScenario(UUID worker, Scenario scenario) { }

    private static Map<WorkerScenario, List<ShiftSaleHistory>> groups(ShiftSaleHistoryRange history,
            Map<ShiftSaleIdentity, ShiftClassification> periods) {
        var comparator = Comparator.comparing((WorkerScenario key) -> key.worker().toString())
                .thenComparing(key -> key.scenario().weekday()).thenComparing(key -> key.scenario().period())
                .thenComparing(key -> key.scenario().dayType()).thenComparing(key -> key.scenario().workedCode()).thenComparing(key -> key.scenario().effectiveRole());
        var groups = new TreeMap<WorkerScenario, List<ShiftSaleHistory>>(comparator);
        for (var sale : history.sales()) {
            groups.computeIfAbsent(new WorkerScenario(sale.workerId(), scenario(sale, periods)),
                    ignored -> new ArrayList<>()).add(sale);
        }
        return groups;
    }

    private static Scenario scenario(ShiftSaleHistory sale, Map<ShiftSaleIdentity, ShiftClassification> periods) {
        return new Scenario(sale.journal().journalDate().getDayOfWeek(), periods.get(sale.identity()).period(),
                periods.get(sale.identity()).dayType(), sale.workedCode(), sale.effectiveRole());
    }

    private static <K, V> Map<K, V> unique(List<V> values, Function<V, K> key) {
        var result = new HashMap<K, V>();
        for (var value : values) {
            if (result.putIfAbsent(key.apply(value), value) != null) {
                throw new IllegalArgumentException("Ambiguous repeated explicit parameter: " + key.apply(value));
            }
        }
        return result;
    }

    private static Window window(ShiftSaleHistoryRange history) { return new Window(history.from(), history.to()); }
    private static List<Coverage> coverage(ShiftSaleHistoryRange history) {
        return history.days().stream().map(day -> new Coverage(day.date(), day.status())).toList();
    }
    private static BigDecimal sum(List<ShiftSaleHistory> rows, Measure measure) {
        return rows.stream().map(measure::value).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    private static BigDecimal average(BigDecimal total, int count, int scale) {
        return total.divide(BigDecimal.valueOf(count), scale, RoundingMode.HALF_EVEN);
    }
    private static Availability availability(List<Availability> rows) {
        long available = rows.stream().filter(value -> value == AVAILABLE).count();
        return available == rows.size() ? AVAILABLE : available == 0 ? UNAVAILABLE : PARTIAL;
    }
    private static Reason reportReason(Availability availability) {
        return availability == AVAILABLE ? null : Reason.INCOMPLETE_CALCULATIONS;
    }
}
