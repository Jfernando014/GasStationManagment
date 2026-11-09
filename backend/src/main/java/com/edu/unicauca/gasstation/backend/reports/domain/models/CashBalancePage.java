package com.edu.unicauca.gasstation.backend.reports.domain.models;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Data of the "saldo en caja" page of the PDF. It is the own model of the reports module, so the
 * code that draws the page does not depend on the records of the sales module.
 * Amounts are in COP. The daily cash, the Dominus accumulated balance and the difference are null
 * while their source does not exist yet, and the page must show them empty.
 *
 * @author javiersolanop777
 */
public record CashBalancePage(
        LocalDate atrDate,
        long atrJournalConsecutive,
        String atrJournalStatus,
        SavingsFund atrSavingsFund,
        Expenses atrExpenses,
        BigDecimal atrCashBalance,
        BigDecimal atrDailyCash,
        BigDecimal atrDominusAccumulatedBalance,
        BigDecimal atrDifference) {

    /** Rent savings fund of the day. */
    public record SavingsFund(BigDecimal atrPreviousBalance, BigDecimal atrContribution,
                              BigDecimal atrPayments, BigDecimal atrAccumulatedBalance) {
    }

    /** One administrative expense of the day. */
    public record ExpenseItem(LocalDate atrDate, String atrDescription, BigDecimal atrAmount) {
    }

    /** Administrative expenses of the day and their total. */
    public record Expenses(List<ExpenseItem> atrItems, BigDecimal atrTotal) {
    }
}
