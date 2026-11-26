package com.edu.unicauca.gasstation.backend.inventory.domain.models;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

/** Fuel variation of a month, with the single alert setting applied to the whole month. */
@Getter
@Builder
public class MonthlyVariation {
    private final YearMonth month;
    private final LocalDate referenceDate;
    private final VariationAlertSetting alertSetting;
    private final List<FuelMonthlyVariation> fuels;
}
