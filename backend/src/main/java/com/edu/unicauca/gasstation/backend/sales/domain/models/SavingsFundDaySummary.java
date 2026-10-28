package com.edu.unicauca.gasstation.backend.sales.domain.models;

import java.math.BigDecimal;

/**
 * Movement of the savings fund on one day. Pure domain model.
 *
 * @param previousBalance balance at the end of the previous day
 * @param contribution    amount saved on the day
 * @param payments        amount paid out of the fund on the day (positive)
 */
public record SavingsFundDaySummary(BigDecimal previousBalance, BigDecimal contribution, BigDecimal payments) {

    /** Balance at the end of the day: previous balance plus the contribution minus the payments. */
    public BigDecimal accumulatedBalance() {
        return previousBalance.add(contribution).subtract(payments);
    }
}
