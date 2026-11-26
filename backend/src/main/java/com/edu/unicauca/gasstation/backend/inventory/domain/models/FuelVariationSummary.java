package com.edu.unicauca.gasstation.backend.inventory.domain.models;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

/** State of a fuel in the month. */
@Getter
@Builder
public class FuelVariationSummary {
    /** Cumulative status of the last calculated day; null when there is none. */
    private final AlertLevel cumulativeStatus;
    private final LocalDate lastCalculatedDate;
    /** Days with status OK. */
    private final int calculatedDays;
    private final int outOfToleranceDays;
}
