package com.edu.unicauca.gasstation.backend.statistics.domain.models;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Reported values of one seller close, with the unit explicit in every component.
 * Fuel volumes are gallons; basket, tirilla and delivered amounts are independent COP observations.
 * Preserves each value and scale, including signed values, without rounding, conversion, totals or reconciliation.
 * Missing values are rejected rather than replaced with zero. Adjustment policy is outside this snapshot.
 */
public record ShiftSaleAmounts(
        BigDecimal motorGallons,
        BigDecimal dieselGallons,
        BigDecimal extraGallons,
        BigDecimal maxproGallons,
        BigDecimal basketDominusAmountCop,
        BigDecimal dominusTirillaCop,
        BigDecimal totalDeliveredCop) {

    public ShiftSaleAmounts {
        Objects.requireNonNull(motorGallons, "motorGallons");
        Objects.requireNonNull(dieselGallons, "dieselGallons");
        Objects.requireNonNull(extraGallons, "extraGallons");
        Objects.requireNonNull(maxproGallons, "maxproGallons");
        Objects.requireNonNull(basketDominusAmountCop, "basketDominusAmountCop");
        Objects.requireNonNull(dominusTirillaCop, "dominusTirillaCop");
        Objects.requireNonNull(totalDeliveredCop, "totalDeliveredCop");
    }
}
