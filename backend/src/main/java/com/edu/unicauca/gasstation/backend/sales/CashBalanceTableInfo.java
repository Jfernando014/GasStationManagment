package com.edu.unicauca.gasstation.backend.sales;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * Cash balance table of a journal, as published by the sales module (table 4c of the daily flow).
 * Amounts are in COP.
 *
 * <p>The last three amounts are {@code null} while their source does not exist yet, so consumers can be written
 * now and keep working when those values start to arrive:
 *
 * @param date                      date of the journal
 * @param journalConsecutive        consecutive number of the journal ("Planilla No.")
 * @param journalStatus             name of the journal status (for example OPEN or CLOSED)
 * @param savingsFund               rent savings fund of the day
 * @param expenses                  administrative expenses of the day ("Recibos, vales y otros")
 * @param cashBalance               cash balance accumulated up to and including {@code date}
 * @param dailyCash                 station cash of the day, taken from the sellers' close (step 2);
 *                                  null until step 2 is published
 * @param dominusAccumulatedBalance accumulated balance pasted from Dominus; null until it is stored
 * @param difference                difference against Dominus; null while any of its sources is missing
 */
public record CashBalanceTableInfo(
        LocalDate date,
        Long journalConsecutive,
        String journalStatus,
        SavingsFundInfo savingsFund,
        ExpensesInfo expenses,
        BigDecimal cashBalance,
        BigDecimal dailyCash,
        BigDecimal dominusAccumulatedBalance,
        BigDecimal difference) {

    public CashBalanceTableInfo {
        Objects.requireNonNull(date, "date");
        Objects.requireNonNull(journalConsecutive, "journalConsecutive");
        Objects.requireNonNull(journalStatus, "journalStatus");
        Objects.requireNonNull(savingsFund, "savingsFund");
        Objects.requireNonNull(expenses, "expenses");
        Objects.requireNonNull(cashBalance, "cashBalance");
    }

    /**
     * Savings fund of the day: {@code accumulatedBalance = previousBalance + contribution - payments}.
     *
     * @param previousBalance    fund balance at the end of the previous day
     * @param contribution       amount saved on the day
     * @param payments           amount paid out of the fund on the day
     * @param accumulatedBalance fund balance at the end of the day
     */
    public record SavingsFundInfo(BigDecimal previousBalance, BigDecimal contribution, BigDecimal payments,
                                  BigDecimal accumulatedBalance) {
    }

    /** One administrative expense of the day. */
    public record ExpenseItemInfo(LocalDate date, String description, BigDecimal amount) {
    }

    /** Expenses of the day and their total; an empty list and a zero total when there are none. */
    public record ExpensesInfo(List<ExpenseItemInfo> items, BigDecimal total) {

        public ExpensesInfo {
            items = List.copyOf(items);
            Objects.requireNonNull(total, "total");
        }
    }
}
