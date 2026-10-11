package com.edu.unicauca.gasstation.backend.sales.domain.models;

/**
 * Period of a configured target. A null month means annual; a month from 1 to 12 means monthly.
 * No business restriction on the year or relationship between annual and monthly targets is imposed.
 */
public record SalesTargetPeriod(int year, Integer month) {

    public SalesTargetPeriod {
        if (month != null && (month < 1 || month > 12)) {
            throw new IllegalArgumentException("month must be between 1 and 12");
        }
    }

    public static SalesTargetPeriod annual(int year) {
        return new SalesTargetPeriod(year, null);
    }

    public static SalesTargetPeriod monthly(int year, int month) {
        return new SalesTargetPeriod(year, month);
    }

    public boolean isAnnual() {
        return month == null;
    }

    public boolean isMonthly() {
        return month != null;
    }
}
