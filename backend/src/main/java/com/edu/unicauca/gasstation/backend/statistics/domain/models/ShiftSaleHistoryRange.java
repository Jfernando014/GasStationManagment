package com.edu.unicauca.gasstation.backend.statistics.domain.models;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

/** Complete calendar coverage, not a certification that an absent date had no sales. */
public record ShiftSaleHistoryRange(LocalDate from, LocalDate to, List<Day> days) {
    public ShiftSaleHistoryRange {
        validateDates(from, to);
        days = List.copyOf(days);
        if (days.size() != ChronoUnit.DAYS.between(from, to) + 1) {
            throw new IllegalArgumentException("Every requested date must be represented");
        }
        for (int i = 0; i < days.size(); i++) {
            if (!days.get(i).date().equals(from.plusDays(i))) {
                throw new IllegalArgumentException("Dates must be consecutive and ordered");
            }
        }
    }

    public static void validateDates(LocalDate from, LocalDate to) {
        Objects.requireNonNull(from, "from");
        Objects.requireNonNull(to, "to");
        if (from.getYear() < 1 || to.getYear() > 9999 || to.isBefore(from)
                || ChronoUnit.DAYS.between(from, to) >= 3660) {
            throw new IllegalArgumentException("Inclusive range must contain 1..3660 dates in years 1..9999");
        }
    }

    public boolean complete() {
        return days.stream().allMatch(day -> day.status() == DayStatus.PROCESSED
                || day.status() == DayStatus.PROCESSED_EMPTY);
    }

    public List<ShiftSaleHistory> sales() {
        return days.stream().flatMap(day -> day.sales().stream()).toList();
    }

    public enum DayStatus { PROCESSED, PROCESSED_EMPTY, PENDING, ABSENT }

    public record Day(LocalDate date, JournalSalesProcessing processing, List<ShiftSaleHistory> sales) {
        public Day {
            Objects.requireNonNull(date, "date");
            sales = List.copyOf(sales);
            if (processing == null) {
                if (!sales.isEmpty()) throw new IllegalArgumentException("Absent date cannot expose sales");
            } else {
                if (!processing.journal().journalDate().equals(date)) {
                    throw new IllegalArgumentException("Processing date mismatch");
                }
                if (processing.status() == JournalSalesProcessing.Status.PENDING && !sales.isEmpty()) {
                    throw new IllegalArgumentException("Pending date cannot expose partial sales");
                }
                if (processing.status() == JournalSalesProcessing.Status.PROCESSED
                        && processing.saleCount() != sales.size()) {
                    throw new IllegalArgumentException("Confirmed sale count mismatch");
                }
            }
            var identities = new HashSet<ShiftSaleIdentity>();
            var workedShifts = new HashSet<String>();
            for (var sale : sales) {
                if (!sale.journal().equals(processing.journal()) || !identities.add(sale.identity())
                        || !workedShifts.add(sale.workerId() + "/" + sale.workedCode())) {
                    throw new IllegalArgumentException("Invalid or duplicate confirmed seller shift");
                }
            }
        }

        public DayStatus status() {
            if (processing == null) return DayStatus.ABSENT;
            if (processing.status() == JournalSalesProcessing.Status.PENDING) return DayStatus.PENDING;
            return sales.isEmpty() ? DayStatus.PROCESSED_EMPTY : DayStatus.PROCESSED;
        }
    }
}
