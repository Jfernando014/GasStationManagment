package com.edu.unicauca.gasstation.backend.sales;

import java.time.LocalDate;
import java.util.Optional;

/**
 * Public contract of the sales module for the daily cash balance table ("saldo en caja").
 *
 * <p>Only entry point other modules (reports) may use to read that table. It returns the public records of
 * this package, never the cash, savings fund or expense tables nor any internal class of sales.
 */
public interface CashBalanceQuery {

    /**
     * Cash balance table of one journal.
     *
     * @param date date of the journal (the label of the operating day), not an instant
     * @return the table, or empty when that date has no journal
     * @throws NullPointerException if {@code date} is null
     */
    Optional<CashBalanceTableInfo> findDailyCashBalance(LocalDate date);
}
