/**
 * Sales module: journals, cash, savings fund, administrative expenses, sales targets and Dominus documents.
 *
 * <p>Public API (usable by other modules): {@link com.edu.unicauca.gasstation.backend.sales.CashBalanceQuery},
 * {@link com.edu.unicauca.gasstation.backend.sales.CashBalanceTableInfo},
 * {@link com.edu.unicauca.gasstation.backend.sales.SellerCloseQuery}, and
 * {@link com.edu.unicauca.gasstation.backend.sales.JournalClosedEvent}. Every subpackage is internal.
 */
@org.springframework.modulith.ApplicationModule(displayName = "Sales")
package com.edu.unicauca.gasstation.backend.sales;
