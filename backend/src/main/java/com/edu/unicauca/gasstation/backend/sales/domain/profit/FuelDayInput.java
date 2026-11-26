package com.edu.unicauca.gasstation.backend.sales.domain.profit;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Gallons of a fuel sold on a day, with the figures in force that day. */
public record FuelDayInput(LocalDate date, BigDecimal gallons, FuelUnitValues values) {
}
