package com.edu.unicauca.gasstation.backend.statistics.domain.calculation;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** Historical performance summary for a single worker across confirmed closed journals. */
public final class WorkerPerformance {
    public static final int DISPLAY_SCALE = 2;
    public static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_EVEN;

    private WorkerPerformance() { }

    public enum PerformanceWeekday {
        MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY, HOLIDAY;

        public static PerformanceWeekday from(LocalDate date, boolean holiday) {
            Objects.requireNonNull(date, "date");
            return holiday ? HOLIDAY : PerformanceWeekday.valueOf(date.getDayOfWeek().name());
        }
    }

    public record RolePerformance(int shifts, BigDecimal averageGallons, BigDecimal performancePercent) {
        public RolePerformance {
            if (shifts < 0) {
                throw new IllegalArgumentException("Shifts count cannot be negative");
            }
            if (shifts == 0) {
                if (averageGallons != null || performancePercent != null) {
                    throw new IllegalArgumentException("Metrics must be null when shifts is zero");
                }
            } else {
                Objects.requireNonNull(averageGallons, "averageGallons");
            }
        }

        public static RolePerformance empty() {
            return new RolePerformance(0, null, null);
        }
    }

    public record WeekdayPerformance(
            PerformanceWeekday weekday,
            RolePerformance asHolder,
            RolePerformance asSupport) {
        public WeekdayPerformance {
            Objects.requireNonNull(weekday, "weekday");
            Objects.requireNonNull(asHolder, "asHolder");
            Objects.requireNonNull(asSupport, "asSupport");
        }
    }

    public record MaxSale(BigDecimal gallons, LocalDate date, String shiftCode) {
        public MaxSale {
            Objects.requireNonNull(gallons, "gallons");
            Objects.requireNonNull(date, "date");
            if (shiftCode == null || shiftCode.isBlank()) {
                throw new IllegalArgumentException("shiftCode cannot be blank");
            }
        }
    }

    public record ClosedShiftsCount(int total, int asHolder, int asSupport) {
        public ClosedShiftsCount {
            if (total < 0 || asHolder < 0 || asSupport < 0) {
                throw new IllegalArgumentException("Shift counts cannot be negative");
            }
            if (total != asHolder + asSupport) {
                throw new IllegalArgumentException("Total shifts must equal asHolder + asSupport");
            }
        }
    }

    public record Report(
            LocalDate periodFrom,
            LocalDate periodTo,
            ClosedShiftsCount closedShifts,
            List<WeekdayPerformance> byWeekday,
            MaxSale maxSale) {
        public Report {
            Objects.requireNonNull(closedShifts, "closedShifts");
            Objects.requireNonNull(byWeekday, "byWeekday");
            byWeekday = List.copyOf(byWeekday);
            if (byWeekday.size() != PerformanceWeekday.values().length) {
                throw new IllegalArgumentException("byWeekday must include all 8 day buckets");
            }
            if (closedShifts.total() == 0 && (periodFrom != null || periodTo != null || maxSale != null)) {
                throw new IllegalArgumentException("Empty report cannot carry period dates or maxSale");
            }
            if (closedShifts.total() > 0 && (periodFrom == null || periodTo == null || periodTo.isBefore(periodFrom))) {
                throw new IllegalArgumentException("Valid operational date bounds required when shifts exist");
            }
        }

        public static Report empty() {
            List<WeekdayPerformance> emptyDays = java.util.Arrays.stream(PerformanceWeekday.values())
                    .map(day -> new WeekdayPerformance(day, RolePerformance.empty(), RolePerformance.empty()))
                    .toList();
            return new Report(null, null, new ClosedShiftsCount(0, 0, 0), emptyDays, null);
        }
    }

    public static void validateWorker(UUID workerId) {
        Objects.requireNonNull(workerId, "workerId");
        if (workerId.equals(new UUID(0, 0))) {
            throw new IllegalArgumentException("Worker UUID cannot be zero");
        }
    }
}
