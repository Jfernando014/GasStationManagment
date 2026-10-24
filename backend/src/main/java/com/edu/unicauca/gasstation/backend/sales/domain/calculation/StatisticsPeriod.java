package com.edu.unicauca.gasstation.backend.sales.domain.calculation;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

public enum StatisticsPeriod {
    DAY, WEEK, MONTH;

    public LocalDate start(LocalDate date) {
        return switch (this) {
            case DAY -> date;
            case WEEK -> date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            case MONTH -> date.withDayOfMonth(1);
        };
    }

    public static void validate(LocalDate from, LocalDate to, int scale) {
        if (from == null || to == null || from.getYear() < 1 || to.getYear() > 9999
                || to.isBefore(from) || java.time.temporal.ChronoUnit.DAYS.between(from, to) >= 3660)
            throw new IllegalArgumentException("Inclusive date range must be valid and at most 3660 days (years 1..9999)");
        if (scale < 0 || scale > 12) throw new IllegalArgumentException("outputScale must be 0..12");
    }
}
