package com.edu.unicauca.gasstation.backend.sales.domain.reconciliation;

import java.math.BigDecimal;

/** Accumulated figures of a month (or the opening figures of the previous month). */
public record PeriodTotals(
        BigDecimal mastercard,
        BigDecimal visa,
        BigDecimal amex,
        BigDecimal qr,
        BigDecimal totalEds,
        String source) {

    /** No data at all for the period. */
    public static final PeriodTotals NONE = of(Money.ZERO, Money.ZERO, Money.ZERO, Money.ZERO, "NONE");

    public static PeriodTotals of(BigDecimal mastercard, BigDecimal visa, BigDecimal amex, BigDecimal qr,
                                  String source) {
        BigDecimal mc = Money.amount("mastercard", mastercard);
        BigDecimal vs = Money.amount("visa", visa);
        return new PeriodTotals(mc, vs, Money.amount("amex", amex), Money.amount("qr", qr), mc.add(vs), source);
    }
}
