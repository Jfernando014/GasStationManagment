package com.edu.unicauca.gasstation.backend.sales.domain.profit;

import java.time.LocalDate;

/**
 * A figure that is not available.
 *
 * @param since first day with gallons sold on which it was not in force
 */
public record MissingData(MissingDatum datum, LocalDate since) {
}
