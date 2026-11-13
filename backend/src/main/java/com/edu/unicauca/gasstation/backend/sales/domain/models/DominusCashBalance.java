package com.edu.unicauca.gasstation.backend.sales.domain.models;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Cash balance that Dominus reports at the end of one day ("SALDO ACUMULADO" of its cash complement,
 * {@code accumulatedCash.accumulatedBalance}). Pure domain model.
 *
 * @param date               operating day (the label of the journal)
 * @param accumulatedBalance cash accumulated up to the end of the day according to Dominus
 */
public record DominusCashBalance(LocalDate date, BigDecimal accumulatedBalance) {

    private static final int MONEY_SCALE = 2;

    public DominusCashBalance {
        Objects.requireNonNull(date, "date");
        Objects.requireNonNull(accumulatedBalance, "accumulatedBalance");
        accumulatedBalance = accumulatedBalance.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }
}
