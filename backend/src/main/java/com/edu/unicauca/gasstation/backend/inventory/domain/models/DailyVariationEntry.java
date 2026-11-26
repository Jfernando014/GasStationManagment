package com.edu.unicauca.gasstation.backend.inventory.domain.models;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

/** A calendar day of the monthly variation of a fuel. Null means unknown, never zero. */
@Getter
@Builder
public class DailyVariationEntry {
    private final LocalDate date;
    private final VariationStatus status;
    private final BigDecimal initialGallons;
    private final BigDecimal entriesGallons;
    private final BigDecimal soldGallons;
    private final BigDecimal theoreticalGallons;
    private final BigDecimal measuredGallons;
    /** measured - theoretical. */
    private final BigDecimal varianceGallons;
    /** variance / sold x 100. Null when nothing was sold. */
    private final BigDecimal variancePercentage;
    private final BigDecimal toleranceGallons;
    private final Boolean outOfTolerance;
    /** The cumulative figures only exist on the days with status OK, the only ones that add up. */
    private final BigDecimal cumulativeSoldGallons;
    private final BigDecimal cumulativeVarianceGallons;
    private final BigDecimal cumulativePercentage;
    private final AlertLevel cumulativeStatus;
}
