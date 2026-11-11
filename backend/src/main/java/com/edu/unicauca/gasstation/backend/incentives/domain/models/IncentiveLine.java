package com.edu.unicauca.gasstation.backend.incentives.domain.models;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Units of one additive that earned the same value per unit. An additive whose rule changed during the month
 * has one line per rule. The period is the validity of that rule cut to the month: {@code periodStart} is the later
 * of the rule's start and the first day of the month, and {@code periodEnd} is the day before the next rule when it
 * starts inside the month, or null when the value holds through the end of the month.
 */
public record IncentiveLine(String sku, String productName, int units, BigDecimal valuePerUnit,
                            BigDecimal subtotal, LocalDate periodStart, LocalDate periodEnd) {
}
