package com.edu.unicauca.gasstation.backend.reports.application;

import com.edu.unicauca.gasstation.backend.reports.domain.models.CashBalancePage;
import com.edu.unicauca.gasstation.backend.reports.exception.CashBalanceNotFoundException;
import com.edu.unicauca.gasstation.backend.sales.CashBalanceQuery;
import com.edu.unicauca.gasstation.backend.sales.CashBalanceTableInfo;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Gets the cash balance table ("saldo en caja") of a journal from the sales module and gives it
 * to the reports module as a CashBalancePage, ready to build the page of the PDF.
 * It only uses the public API of sales (CashBalanceQuery and CashBalanceTableInfo), as the
 * architecture test SalesPublicApiBoundaryTest requires.
 *
 * @author javiersolanop777
 */
@Service
public class CashBalancePageService {

    private final CashBalanceQuery atrCashBalanceQuery;

    public CashBalancePageService(CashBalanceQuery prmCashBalanceQuery) {
        this.atrCashBalanceQuery = prmCashBalanceQuery;
    }

    /**
     * Page data of the journal of a date.
     *
     * @throws IllegalArgumentException     if the date is null
     * @throws CashBalanceNotFoundException if there is no journal for that date
     */
    public CashBalancePage getPage(LocalDate prmDate) {
        if (prmDate == null) {
            throw new IllegalArgumentException("The date is required");
        }
        CashBalanceTableInfo objTable = atrCashBalanceQuery.findDailyCashBalance(prmDate)
                .orElseThrow(() -> new CashBalanceNotFoundException(prmDate));
        return toPage(objTable);
    }

    /** Copies every field. The values that are null in the source stay null. */
    private static CashBalancePage toPage(CashBalanceTableInfo prmTable) {
        CashBalanceTableInfo.SavingsFundInfo objFund = prmTable.savingsFund();
        List<CashBalancePage.ExpenseItem> objItems = prmTable.expenses().items().stream()
                .map(prmItem -> new CashBalancePage.ExpenseItem(prmItem.date(), prmItem.description(), prmItem.amount()))
                .toList();
        return new CashBalancePage(
                prmTable.date(),
                prmTable.journalConsecutive(),
                prmTable.journalStatus(),
                new CashBalancePage.SavingsFund(objFund.previousBalance(), objFund.contribution(),
                        objFund.payments(), objFund.accumulatedBalance()),
                new CashBalancePage.Expenses(objItems, prmTable.expenses().total()),
                prmTable.cashBalance(),
                prmTable.dailyCash(),
                prmTable.dominusAccumulatedBalance(),
                prmTable.difference());
    }
}
