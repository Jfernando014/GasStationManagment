package com.edu.unicauca.gasstation.backend.incentives.domain.services;

import com.edu.unicauca.gasstation.backend.incentives.domain.models.AccumulationStart;
import com.edu.unicauca.gasstation.backend.incentives.domain.models.AdditiveSaleUnits;
import com.edu.unicauca.gasstation.backend.incentives.domain.models.IncentiveLine;
import com.edu.unicauca.gasstation.backend.incentives.domain.models.IncentiveRule;
import com.edu.unicauca.gasstation.backend.incentives.domain.models.MonthlyIncentives;
import com.edu.unicauca.gasstation.backend.incentives.domain.models.RuleTimeline;
import com.edu.unicauca.gasstation.backend.incentives.domain.models.UnresolvedAdditive;
import com.edu.unicauca.gasstation.backend.incentives.domain.models.UnresolvedReason;
import com.edu.unicauca.gasstation.backend.incentives.domain.models.VendorIncentive;
import com.edu.unicauca.gasstation.backend.incentives.domain.ports.AdditiveCatalogPort;
import com.edu.unicauca.gasstation.backend.incentives.domain.ports.AdditiveInfo;
import com.edu.unicauca.gasstation.backend.incentives.domain.repositories.AdditiveSaleRepository;
import com.edu.unicauca.gasstation.backend.incentives.domain.repositories.IncentiveRuleRepository;
import com.edu.unicauca.gasstation.backend.incentives.exception.IncentiveError;
import com.edu.unicauca.gasstation.backend.incentives.exception.IncentiveException;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Calculates the incentive of each seller for a month from the additive units stored per day. Every day uses the
 * rule in force that day, so a change in the middle of the month is split. The accumulated total adds every month
 * from the configured start up to the month asked. "Today" is Bogotá time.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class IncentiveCalculationService {

    private final AdditiveSaleRepository saleRepository;
    private final IncentiveRuleRepository ruleRepository;
    private final AdditiveCatalogPort catalog;
    private final UnitValueCalculator unitValueCalculator;
    private final AccumulationStart accumulationStart;
    private final Clock clock;

    /** Every seller who sold additives in the month. An empty list is a valid answer. */
    public MonthlyIncentives calculateMonth(YearMonth month) {
        requireCalculable(month);
        List<AdditiveSaleUnits> sales = saleRepository.findBetween(firstDay(), month.atEndOfMonth());
        return new MonthlyIncentives(month, accumulationStart.month(), calculate(month, sales, false));
    }

    /**
     * One seller by document. A seller without sales in the month still gets an answer with the accumulated total
     * of the earlier months; one without sales at all gets zeros and no name.
     */
    public MonthlyIncentives calculateVendor(YearMonth month, String vendorDocument) {
        requireCalculable(month);
        String document = vendorDocument.strip();
        List<AdditiveSaleUnits> sales = saleRepository.findBetweenForVendor(firstDay(), month.atEndOfMonth(), document);
        VendorIncentive vendor = calculate(month, sales, true).stream().findFirst()
                .orElseGet(() -> new VendorIncentive(document, null, List.of(), List.of(), BigDecimal.ZERO,
                        BigDecimal.ZERO, 0));
        return new MonthlyIncentives(month, accumulationStart.month(), List.of(vendor));
    }

    private List<VendorIncentive> calculate(YearMonth month, List<AdditiveSaleUnits> sales,
                                            boolean includeWithoutMonthSales) {
        Map<String, Long> productIdBySku = resolveProducts(sales);
        Map<Long, RuleTimeline> timelines = ruleRepository.findByProductIds(Set.copyOf(productIdBySku.values()))
                .stream()
                .collect(Collectors.groupingBy(IncentiveRule::productId,
                        Collectors.collectingAndThen(Collectors.toList(), RuleTimeline::of)));

        // Sales come ordered by date, so the name kept for a seller is the most recent one
        Map<String, VendorTotals> vendors = new LinkedHashMap<>();
        for (AdditiveSaleUnits sale : sales) {
            VendorTotals totals = vendors.computeIfAbsent(sale.vendorDocument(), VendorTotals::new);
            totals.vendorName = sale.vendorName();
            boolean inMonth = YearMonth.from(sale.journalDate()).equals(month);
            totals.hasSalesInMonth |= inMonth;

            Long productId = productIdBySku.get(sale.sku());
            if (productId == null) {
                totals.addUnresolved(sale, UnresolvedReason.PRODUCT_NOT_FOUND, inMonth);
                continue;
            }
            RuleTimeline rules = timelines.getOrDefault(productId, RuleTimeline.of(List.of()));
            Optional<BigDecimal> value = unitValueCalculator.valuePerUnit(rules, sale);
            if (value.isEmpty()) {
                totals.addUnresolved(sale, UnresolvedReason.NO_RULE_IN_FORCE, inMonth);
                continue;
            }
            BigDecimal subtotal = value.get().multiply(BigDecimal.valueOf(sale.units()));
            totals.accumulated = totals.accumulated.add(subtotal);
            if (inMonth) {
                totals.addLine(sale, value.get(), subtotal, periodOf(rules, sale.journalDate(), month));
            }
        }
        return vendors.values().stream()
                .filter(totals -> includeWithoutMonthSales || totals.hasSalesInMonth)
                .map(VendorTotals::toVendorIncentive)
                .sorted(Comparator.comparing(VendorIncentive::vendorName, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(VendorIncentive::vendorDocument))
                .toList();
    }

    /** One catalog lookup per distinct SKU; SKUs without a product are left out of the map. */
    private Map<String, Long> resolveProducts(List<AdditiveSaleUnits> sales) {
        Map<String, Long> productIdBySku = new HashMap<>();
        for (String sku : sales.stream().map(AdditiveSaleUnits::sku).collect(Collectors.toSet())) {
            catalog.findByCode(sku).map(AdditiveInfo::id).ifPresent(id -> productIdBySku.put(sku, id));
        }
        return productIdBySku;
    }

    private void requireCalculable(YearMonth month) {
        YearMonth current = YearMonth.now(clock);
        if (month.isAfter(current)) {
            throw new IncentiveException(IncentiveError.MONTH_IN_FUTURE, month, current);
        }
        if (month.isBefore(accumulationStart.month())) {
            throw new IncentiveException(IncentiveError.MONTH_BEFORE_START, month, accumulationStart.month());
        }
    }

    private LocalDate firstDay() {
        return accumulationStart.month().atDay(1);
    }

    /**
     * Validity of the rule in force on {@code saleDate}, cut to the month: it starts on the later of the rule's start
     * and the first day of the month, and ends the day before the next rule if that one starts inside the month.
     * Without a change inside the month the end is null, meaning it holds through the end of the month asked.
     */
    private static LinePeriod periodOf(RuleTimeline rules, LocalDate saleDate, YearMonth month) {
        LocalDate firstDay = month.atDay(1);
        LocalDate start = rules.effectiveOn(saleDate).map(IncentiveRule::validFrom)
                .filter(ruleStart -> ruleStart.isAfter(firstDay)).orElse(firstDay);
        LocalDate end = rules.nextAfter(saleDate).map(IncentiveRule::validFrom)
                .filter(nextStart -> !nextStart.isAfter(month.atEndOfMonth()))
                .map(nextStart -> nextStart.minusDays(1)).orElse(null);
        return new LinePeriod(start, end);
    }

    private record LinePeriod(LocalDate start, LocalDate end) {
    }

    private record LineKey(String sku, BigDecimal valuePerUnit, LinePeriod period) {
    }

    private record UnresolvedKey(String sku, UnresolvedReason reason) {
    }

    private static final class LineTotals {
        private final String productName;
        private int units;
        private BigDecimal subtotal = BigDecimal.ZERO;

        private LineTotals(String productName) {
            this.productName = productName;
        }
    }

    private static final class UnresolvedTotals {
        private final String productName;
        private int units;

        private UnresolvedTotals(String productName) {
            this.productName = productName;
        }
    }

    /** Running totals of one seller while the sales are walked once. */
    private static final class VendorTotals {
        private final String vendorDocument;
        private String vendorName;
        private boolean hasSalesInMonth;
        private BigDecimal accumulated = BigDecimal.ZERO;
        private int accumulatedUnresolvedUnits;
        private final Map<LineKey, LineTotals> lines = new LinkedHashMap<>();
        private final Map<UnresolvedKey, UnresolvedTotals> unresolved = new LinkedHashMap<>();

        private VendorTotals(String vendorDocument) {
            this.vendorDocument = vendorDocument;
        }

        // The period is part of the key, so equal values from two different rules stay on separate lines
        private void addLine(AdditiveSaleUnits sale, BigDecimal valuePerUnit, BigDecimal subtotal,
                             LinePeriod period) {
            LineTotals line = lines.computeIfAbsent(new LineKey(sale.sku(), valuePerUnit, period),
                    key -> new LineTotals(sale.productName()));
            line.units += sale.units();
            line.subtotal = line.subtotal.add(subtotal);
        }

        // Every unresolved unit counts against the accumulated total; only the month's ones are listed
        private void addUnresolved(AdditiveSaleUnits sale, UnresolvedReason reason, boolean inMonth) {
            accumulatedUnresolvedUnits += sale.units();
            if (inMonth) {
                unresolved.computeIfAbsent(new UnresolvedKey(sale.sku(), reason),
                        key -> new UnresolvedTotals(sale.productName())).units += sale.units();
            }
        }

        private VendorIncentive toVendorIncentive() {
            List<IncentiveLine> incentiveLines = lines.entrySet().stream()
                    .map(entry -> new IncentiveLine(entry.getKey().sku(), entry.getValue().productName,
                            entry.getValue().units, entry.getKey().valuePerUnit(), entry.getValue().subtotal,
                            entry.getKey().period().start(), entry.getKey().period().end()))
                    .sorted(Comparator.comparing(IncentiveLine::productName)
                            .thenComparing(IncentiveLine::sku)
                            .thenComparing(IncentiveLine::valuePerUnit)
                            .thenComparing(IncentiveLine::periodStart))
                    .toList();
            List<UnresolvedAdditive> unresolvedAdditives = unresolved.entrySet().stream()
                    .map(entry -> new UnresolvedAdditive(entry.getKey().sku(), entry.getValue().productName,
                            entry.getValue().units, entry.getKey().reason()))
                    .sorted(Comparator.comparing(UnresolvedAdditive::sku)
                            .thenComparing(item -> item.reason().name()))
                    .toList();
            BigDecimal monthTotal = incentiveLines.stream().map(IncentiveLine::subtotal)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            return new VendorIncentive(Objects.requireNonNull(vendorDocument), vendorName, incentiveLines,
                    unresolvedAdditives, monthTotal, accumulated, accumulatedUnresolvedUnits);
        }
    }
}
