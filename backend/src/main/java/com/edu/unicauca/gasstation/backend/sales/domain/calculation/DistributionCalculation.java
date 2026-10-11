package com.edu.unicauca.gasstation.backend.sales.domain.calculation;

import com.edu.unicauca.gasstation.backend.sales.domain.models.SalesTarget;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

public final class DistributionCalculation {
    private DistributionCalculation() { }
    /** Date is the worksheet/calendar row label, not an inferred night-shift operational date. */
    public record Assignment(LocalDate date, UUID workerId, String shiftCode) {
        public Assignment { Objects.requireNonNull(date); Objects.requireNonNull(workerId); if (shiftCode == null || shiftCode.isBlank()) throw new IllegalArgumentException("Shift code required"); }
    }
    public record Coverage(Assignment assignment, Set<Integer> hours) {
        public Coverage {
            Objects.requireNonNull(assignment); hours = Set.copyOf(hours);
            for (int hour : hours) if (hour < 0 || hour > 23) throw new IllegalArgumentException("Hour must be 0..23");
        }
    }
    public record Input(SalesTarget target, DistributionProfile profile, List<Coverage> coverage, int outputScale) {
        public Input { Objects.requireNonNull(target); Objects.requireNonNull(profile); coverage = List.copyOf(coverage); }
    }
    public record HourKey(LocalDate date, int hour) implements Comparable<HourKey> {
        @Override public int compareTo(HourKey other) { int cmp = date.compareTo(other.date); return cmp == 0 ? Integer.compare(hour, other.hour) : cmp; }
    }
    public record Allocation(LocalDate date, int hour, UUID workerId, BigDecimal official, BigDecimal reference) { }
    public record Totals(BigDecimal official, BigDecimal reference) {
        public Totals add(Totals other) { return new Totals(official.add(other.official), reference.add(other.reference)); }
    }
    public record Result(SalesTarget target, int outputScale, String decimalPolicy, String parameterSource,
            Totals monthlyTotal, List<Allocation> allocations, Map<LocalDate, Totals> byDate, Map<HourKey, Totals> byHour,
            Map<UUID, Totals> byWorker, List<HourKey> uncoveredHours, String assignmentSource) {
        public Result {
            allocations = List.copyOf(allocations); byDate = Collections.unmodifiableMap(new TreeMap<>(byDate));
            byHour = Collections.unmodifiableMap(new TreeMap<>(byHour)); byWorker = Collections.unmodifiableMap(new TreeMap<>(byWorker));
            uncoveredHours = List.copyOf(uncoveredHours);
        }
    }
}
