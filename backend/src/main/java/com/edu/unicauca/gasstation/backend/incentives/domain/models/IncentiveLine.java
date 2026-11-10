package com.edu.unicauca.gasstation.backend.incentives.domain.models;

import java.math.BigDecimal;

/**
 * Units of one additive that earned the same value per unit. An additive whose rule changed during the month
 * has one line per value.
 */
public record IncentiveLine(String sku, String productName, int units, BigDecimal valuePerUnit,
                            BigDecimal subtotal) {
}
