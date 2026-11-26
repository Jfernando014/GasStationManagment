package com.edu.unicauca.gasstation.backend.inventory.domain.models;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Month totals of a fuel. Null means that there is nothing to add up. */
@Getter
@Builder
public class MonthlyVariationTotals {
    /** Every recorded day. */
    private final BigDecimal entriesGallons;
    /** Every recorded day. */
    private final BigDecimal soldGallons;
    /** Only the days with status OK. */
    private final BigDecimal varianceGallons;
    /** Variance divided by the sales of the days with status OK, as the cumulative percentage of the last one. */
    private final BigDecimal variancePercentage;
    /** Over the days on which something was sold. */
    private final BigDecimal averageDailySoldGallons;
    private final BigDecimal lastMeasuredGallons;
    private final LocalDate lastMeasuredDate;
}
