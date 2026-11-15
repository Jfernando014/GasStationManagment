package com.edu.unicauca.gasstation.backend.reports.application;

import com.edu.unicauca.gasstation.backend.reports.domain.models.BalanceReport;
import com.edu.unicauca.gasstation.backend.reports.domain.models.BalanceReport.FuelVariationAggregate;
import com.edu.unicauca.gasstation.backend.reports.domain.models.BalanceReport.SellerAggregate;
import com.edu.unicauca.gasstation.backend.reports.domain.models.ReportDay;
import com.edu.unicauca.gasstation.backend.reports.domain.models.ReportDay.Source;
import com.edu.unicauca.gasstation.backend.reports.exception.InvalidReportPeriodException;
import com.edu.unicauca.gasstation.backend.reports.exception.ReportNoDataException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;

/**
 * Builds the executive balance of a period from the normalized report days.
 *
 * @author javiersolanop777
 */
@Service
public class BalanceReportService {

    static final String NO_DATA = "No hay información suficiente para generar el balance de este período";
    private static final long MAX_PERIOD_DAYS = 366;

    private final ReportDataService reportDataService;

    public BalanceReportService(ReportDataService reportDataService) {
        this.reportDataService = reportDataService;
    }

    public BalanceReport build(LocalDate from, LocalDate to) {
        Objects.requireNonNull(from, "from");
        Objects.requireNonNull(to, "to");
        if (from.isAfter(to)) {
            throw new InvalidReportPeriodException("La fecha inicial no puede ser posterior a la fecha final");
        }
        if (ChronoUnit.DAYS.between(from, to) + 1 > MAX_PERIOD_DAYS) {
            throw new InvalidReportPeriodException("El período no puede superar " + MAX_PERIOD_DAYS + " días");
        }

        List<ReportDay> days = reportDataService.findDays(from, to);
        if (days.isEmpty()) {
            throw new ReportNoDataException(NO_DATA);
        }

        int liveDays = (int) days.stream().filter(day -> day.source() == Source.LIVE).count();
        int migratedDays = (int) days.stream().filter(day -> day.source() == Source.MIGRATED).count();

        BigDecimal totalGallons = sum(days.stream().map(ReportDay::totalGallons).toList());
        BigDecimal totalDelivered = sum(days.stream().map(ReportDay::totalDelivered).toList());
        BigDecimal totalCash = sumNullable(days.stream().map(ReportDay::totalCash).toList());
        BigDecimal totalBasket = sum(days.stream().map(ReportDay::totalBasket).toList());
        BigDecimal totalDiscrepancy = sum(days.stream().map(ReportDay::totalDiscrepancy).toList());

        return new BalanceReport(from, to, days, liveDays, migratedDays, totalGallons, totalDelivered,
                totalCash, totalBasket, totalDiscrepancy, aggregateVariations(days), aggregateSellers(days),
                lastCash(days));
    }

    private static List<FuelVariationAggregate> aggregateVariations(List<ReportDay> days) {
        Map<String, Accumulator> byFuel = new LinkedHashMap<>();
        for (ReportDay day : days) {
            for (ReportDay.FuelVariation variation : day.variations()) {
                byFuel.computeIfAbsent(variation.fuelType(), ignored -> new Accumulator())
                        .add(variation);
            }
        }
        return byFuel.entrySet().stream()
                .map(entry -> entry.getValue().toAggregate(entry.getKey()))
                .sorted(Comparator.comparing(FuelVariationAggregate::fuelType))
                .toList();
    }

    private static List<SellerAggregate> aggregateSellers(List<ReportDay> days) {
        Map<String, SellerAccumulator> bySeller = new LinkedHashMap<>();
        for (ReportDay day : days) {
            for (ReportDay.VendorRow vendor : day.vendors()) {
                String key = vendor.vendorDocument() == null ? vendor.vendorName() : vendor.vendorDocument();
                bySeller.computeIfAbsent(key, ignored -> new SellerAccumulator(vendor))
                        .add(vendor);
            }
        }
        return bySeller.values().stream()
                .map(SellerAccumulator::toAggregate)
                .sorted(Comparator.comparing(SellerAggregate::totalDelivered,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    private static ReportDay.CashSummary lastCash(List<ReportDay> days) {
        for (int index = days.size() - 1; index >= 0; index--) {
            if (days.get(index).cash() != null) {
                return days.get(index).cash();
            }
        }
        return null;
    }

    private static BigDecimal sum(List<BigDecimal> values) {
        BigDecimal total = BigDecimal.ZERO;
        for (BigDecimal value : values) {
            total = total.add(value == null ? BigDecimal.ZERO : value);
        }
        return total;
    }

    private static BigDecimal sumNullable(List<BigDecimal> values) {
        BigDecimal total = null;
        for (BigDecimal value : values) {
            if (value != null) {
                total = total == null ? value : total.add(value);
            }
        }
        return total;
    }

    private static final class Accumulator {
        private BigDecimal theoretical = BigDecimal.ZERO;
        private BigDecimal measured = BigDecimal.ZERO;
        private BigDecimal variance = BigDecimal.ZERO;
        private BigDecimal tolerance = BigDecimal.ZERO;
        private boolean outOfTolerance;
        private int days;

        void add(ReportDay.FuelVariation variation) {
            theoretical = theoretical.add(zero(variation.theoreticalGallons()));
            measured = measured.add(zero(variation.measuredGallons()));
            variance = variance.add(zero(variation.varianceGallons()));
            tolerance = tolerance.add(zero(variation.toleranceGallons()));
            outOfTolerance = outOfTolerance || variation.outOfTolerance();
            days++;
        }

        FuelVariationAggregate toAggregate(String fuelType) {
            return new FuelVariationAggregate(fuelType, theoretical, measured, variance, tolerance,
                    outOfTolerance, days);
        }

        private static BigDecimal zero(BigDecimal value) {
            return value == null ? BigDecimal.ZERO : value;
        }
    }

    private static final class SellerAccumulator {
        private final String document;
        private final String name;
        private BigDecimal gallons = BigDecimal.ZERO;
        private BigDecimal delivered = BigDecimal.ZERO;
        private BigDecimal basket = BigDecimal.ZERO;
        private BigDecimal discrepancy = BigDecimal.ZERO;

        SellerAccumulator(ReportDay.VendorRow first) {
            this.document = first.vendorDocument();
            this.name = first.vendorName();
        }

        void add(ReportDay.VendorRow vendor) {
            gallons = gallons.add(zero(vendor.gallons()));
            delivered = delivered.add(zero(vendor.totalDelivered()));
            basket = basket.add(zero(vendor.basketAmount()));
            discrepancy = discrepancy.add(zero(vendor.discrepancy()));
        }

        SellerAggregate toAggregate() {
            return new SellerAggregate(document, name, gallons, delivered, basket, discrepancy);
        }

        private static BigDecimal zero(BigDecimal value) {
            return value == null ? BigDecimal.ZERO : value;
        }
    }
}
