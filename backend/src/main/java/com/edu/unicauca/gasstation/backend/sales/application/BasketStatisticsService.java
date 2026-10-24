package com.edu.unicauca.gasstation.backend.sales.application;

import com.edu.unicauca.gasstation.backend.inventory.ProductCategory;
import com.edu.unicauca.gasstation.backend.sales.BasketSalesReadPort;
import com.edu.unicauca.gasstation.backend.sales.BasketSalesReadPort.Sale;
import com.edu.unicauca.gasstation.backend.sales.WorkedShiftsReadPort;
import com.edu.unicauca.gasstation.backend.sales.WorkedShiftsReadPort.WorkedShift;
import com.edu.unicauca.gasstation.backend.sales.api.dto.BasketStatisticsResponse;
import com.edu.unicauca.gasstation.backend.sales.api.dto.BasketStatisticsResponse.*;
import com.edu.unicauca.gasstation.backend.sales.api.dto.StatisticsChartResponse;
import com.edu.unicauca.gasstation.backend.sales.api.dto.StatisticsChartResponse.*;
import com.edu.unicauca.gasstation.backend.sales.domain.calculation.*;
import com.edu.unicauca.gasstation.backend.sales.exception.StatisticsSourceException;
import com.edu.unicauca.gasstation.backend.shifts.ScheduleWorkerDirectory;
import java.math.*;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Supplier;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

/** Read-only orchestration. Missing adapters remain missing; no fallback production data. */
@Service
public class BasketStatisticsService {
    private final ObjectProvider<BasketSalesReadPort> sales;
    private final ObjectProvider<WorkedShiftsReadPort> attendance;
    private final ScheduleWorkerDirectory workers;

    public BasketStatisticsService(ObjectProvider<BasketSalesReadPort> sales,
            ObjectProvider<WorkedShiftsReadPort> attendance, ScheduleWorkerDirectory workers) {
        this.sales = sales;
        this.attendance = attendance;
        this.workers = workers;
    }

    public BasketStatisticsResponse read(LocalDate from, LocalDate to, UUID workerId,
            ProductCategory category, StatisticsPeriod period, int scale) {
        StatisticsPeriod.validate(from, to, scale);
        Objects.requireNonNull(period, "period");
        if (new UUID(0, 0).equals(workerId)) throw new IllegalArgumentException("Valid worker UUID required");
        var port = sales.getIfAvailable();
        if (port == null) return empty(from, to, workerId, category, period, scale, Availability.UNAVAILABLE, "SALES_SOURCE_NOT_CONNECTED");
        var snapshot = readSource(() -> port.read(from, to));
        if (snapshot == null) throw new StatisticsSourceException("Sales source returned no snapshot");
        if (!snapshot.available()) return empty(from, to, workerId, category, period, scale, Availability.UNAVAILABLE, "SALES_SOURCE_UNAVAILABLE");
        var ids = new HashSet<String>();
        for (var row : snapshot.sales()) {
            if (!ids.add(row.id()) || row.date().isBefore(from) || row.date().isAfter(to))
                throw new StatisticsSourceException("Sales source returned duplicate IDs or out-of-range dates");
        }
        var selected = snapshot.sales().stream().filter(s -> (workerId == null || workerId.equals(s.workerId()))
                && (category == null || category == s.category())).toList();
        if (selected.isEmpty()) return empty(from, to, workerId, category, period, scale, Availability.NO_DATA, "NO_RECORDED_SALES_FOR_FILTERS");

        var verified = workedShifts(from, to, workerId, selected);
        var names = workers.getWorkersByIds(selected.stream().map(Sale::workerId).distinct().toList());
        var total = amount(selected);
        record Key(UUID worker, ProductCategory category) { }
        var grouped = new TreeMap<Key, List<Sale>>(Comparator.comparing(Key::worker).thenComparing(Key::category));
        for (var row : selected) grouped.computeIfAbsent(new Key(row.workerId(), row.category()), ignored -> new ArrayList<>()).add(row);
        var groups = new ArrayList<Group>();
        var series = new ArrayList<Series>();
        for (var entry : grouped.entrySet()) {
            var key = entry.getKey();
            var rows = entry.getValue();
            var worker = names.get(key.worker());
            var count = verified.available() ? verified.shifts().stream().filter(s -> key.worker().equals(s.workerId())).count() : null;
            var groupAmount = amount(rows);
            groups.add(new Group(key.worker(), worker == null ? null : worker.fullName(), key.category(), groupAmount,
                    quantity(rows), rows.size(), divide(groupAmount.multiply(BigDecimal.valueOf(100)), total, scale),
                    count, count == null ? null : divide(groupAmount, BigDecimal.valueOf(count), scale)));
            String label = (worker == null ? key.worker().toString() : worker.fullName()) + " / " + key.category();
            for (boolean money : new boolean[]{true, false}) {
                var points = StatisticsSeries.aggregate(rows.stream().map(s -> new StatisticsSeries.Value(s.date(), money ? s.amount() : s.quantity())).toList(), period, scale)
                        .stream().map(b -> new Point(b.start().toString(), b.start(), b.total(), b.observations(), b.change(), b.changePercent())).toList();
                series.add(new Series(key.worker() + "/" + key.category() + (money ? "/amount" : "/quantity"), label,
                        ChartType.LINE, money ? Metric.RECORDED_AMOUNT : Metric.RECORDED_QUANTITY, money ? Unit.COP : Unit.PRODUCT_UNITS, key.worker(), key.category(), points));
            }
        }
        series.add(new Series("seller-category-amount", "Ventas por vendedor y categoría", ChartType.BAR,
                Metric.RECORDED_AMOUNT, Unit.COP, null, null, groups.stream().map(g -> new Point(g.workerId() + "/" + g.category(), null,
                        g.amount(), g.saleLines(), null, null)).toList()));
        series.add(new Series("seller-category-quantity", "Unidades por vendedor y categoría", ChartType.BAR,
                Metric.RECORDED_QUANTITY, Unit.PRODUCT_UNITS, null, null, groups.stream().map(g -> new Point(g.workerId() + "/" + g.category(), null,
                        g.quantity(), g.saleLines(), null, null)).toList()));
        if (total.signum() != 0)
            series.add(new Series("seller-category-participation", "Participación en el total filtrado", ChartType.BAR,
                    Metric.PARTICIPATION, Unit.PERCENT, null, null, groups.stream().map(g -> new Point(g.workerId() + "/" + g.category(), null,
                            g.participationPercent(), g.saleLines(), null, null)).toList()));
        if (verified.available())
            series.add(new Series("seller-category-average", "Promedio por turno efectivamente trabajado", ChartType.BAR,
                    Metric.AVERAGE_PER_WORKED_SHIFT, Unit.COP_PER_WORKED_SHIFT, null, null, groups.stream().map(g -> new Point(g.workerId() + "/" + g.category(), null,
                            g.averageAmountPerWorkedShift(), g.saleLines(), null, null)).toList()));
        Long shiftCount = verified.available() ? (long) verified.shifts().size() : null;
        return new BasketStatisticsResponse(from, to, workerId, category, period, "COP", "PRODUCT_UNITS",
                verified.available() ? Availability.AVAILABLE : Availability.UNAVAILABLE, verified.reason(),
                new Summary(total, quantity(selected), selected.size(), shiftCount,
                        shiftCount == null ? null : divide(total, BigDecimal.valueOf(shiftCount), scale)), groups,
                StatisticsChartResponse.of(Source.REAL_HISTORICAL_SALES, Availability.AVAILABLE, null, from, to, scale, series));
    }

    private record Attendance(boolean available, String reason, List<WorkedShift> shifts) { }

    private Attendance workedShifts(LocalDate from, LocalDate to, UUID workerId, List<Sale> selected) {
        var port = attendance.getIfAvailable();
        if (port == null) return new Attendance(false, "WORKED_SHIFTS_SOURCE_NOT_CONNECTED", List.of());
        var snapshot = readSource(() -> port.read(from, to));
        if (snapshot == null) throw new StatisticsSourceException("Attendance source returned no snapshot");
        if (!snapshot.available()) return new Attendance(false, "WORKED_SHIFTS_SOURCE_UNAVAILABLE", List.of());
        var ids = new HashSet<String>();
        for (var shift : snapshot.shifts()) {
            if (!ids.add(shift.id()) || shift.date().isBefore(from) || shift.date().isAfter(to))
                throw new StatisticsSourceException("Attendance source returned duplicate IDs or out-of-range dates");
        }
        var shifts = snapshot.shifts().stream().filter(s -> workerId == null || workerId.equals(s.workerId())).toList();
        record WorkerDate(UUID worker, LocalDate date) { }
        var days = new HashSet<WorkerDate>();
        shifts.forEach(s -> days.add(new WorkerDate(s.workerId(), s.date())));
        if (selected.stream().anyMatch(s -> !days.contains(new WorkerDate(s.workerId(), s.date()))))
            return new Attendance(false, "SALES_WITHOUT_VERIFIED_WORKED_SHIFT", List.of());
        return new Attendance(true, null, shifts);
    }

    private BasketStatisticsResponse empty(LocalDate from, LocalDate to, UUID workerId, ProductCategory category,
            StatisticsPeriod period, int scale, Availability status, String reason) {
        return new BasketStatisticsResponse(from, to, workerId, category, period, "COP", "PRODUCT_UNITS",
                Availability.UNAVAILABLE, "NOT_EVALUATED_WITHOUT_SALES", null, List.of(),
                StatisticsChartResponse.of(Source.REAL_HISTORICAL_SALES, status, reason, from, to, scale, List.of()));
    }
    private static BigDecimal amount(List<Sale> rows) { return rows.stream().map(Sale::amount).reduce(BigDecimal.ZERO, BigDecimal::add); }
    private static BigDecimal quantity(List<Sale> rows) { return rows.stream().map(Sale::quantity).reduce(BigDecimal.ZERO, BigDecimal::add); }
    private static BigDecimal divide(BigDecimal numerator, BigDecimal denominator, int scale) {
        return denominator.signum() == 0 ? null : numerator.divide(denominator, scale, RoundingMode.HALF_EVEN);
    }
    private static <T> T readSource(Supplier<T> read) {
        try {
            return read.get();
        } catch (IllegalArgumentException | NullPointerException ex) {
            throw new StatisticsSourceException("Connected statistics source returned invalid data", ex);
        }
    }
}
