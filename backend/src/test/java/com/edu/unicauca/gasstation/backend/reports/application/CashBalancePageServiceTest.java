package com.edu.unicauca.gasstation.backend.reports.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.edu.unicauca.gasstation.backend.reports.domain.models.CashBalancePage;
import com.edu.unicauca.gasstation.backend.reports.exception.CashBalanceNotFoundException;
import com.edu.unicauca.gasstation.backend.sales.CashBalanceQuery;
import com.edu.unicauca.gasstation.backend.sales.CashBalanceTableInfo;
import com.edu.unicauca.gasstation.backend.sales.CashBalanceTableInfo.ExpenseItemInfo;
import com.edu.unicauca.gasstation.backend.sales.CashBalanceTableInfo.ExpensesInfo;
import com.edu.unicauca.gasstation.backend.sales.CashBalanceTableInfo.SavingsFundInfo;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * The service gets the table from CashBalanceQuery (here a hand-written fake) and keeps all its data.
 *
 * @author javiersolanop777
 */
class CashBalancePageServiceTest {

    private static final LocalDate DATE = LocalDate.of(2026, 10, 24);

    @Test
    void copiesAllTheDataOfTheTable() {
        CashBalancePageService objService = serviceWith(fullTable());

        CashBalancePage objPage = objService.getPage(DATE);

        assertEquals(DATE, objPage.atrDate());
        assertEquals(15L, objPage.atrJournalConsecutive());
        assertEquals("CLOSED", objPage.atrJournalStatus());
        assertEquals(bd("100000"), objPage.atrSavingsFund().atrPreviousBalance());
        assertEquals(bd("5000"), objPage.atrSavingsFund().atrContribution());
        assertEquals(bd("0"), objPage.atrSavingsFund().atrPayments());
        assertEquals(bd("105000"), objPage.atrSavingsFund().atrAccumulatedBalance());
        assertEquals(bd("1500000"), objPage.atrCashBalance());
        assertEquals(bd("250000"), objPage.atrDailyCash());
        assertEquals(bd("1498000"), objPage.atrDominusAccumulatedBalance());
        assertEquals(bd("2000"), objPage.atrDifference());
    }

    @Test
    void keepsTheExpensesInTheSameOrderWithTheirTotal() {
        CashBalancePage objPage = serviceWith(fullTable()).getPage(DATE);

        assertEquals(2, objPage.atrExpenses().atrItems().size());
        assertEquals("Papelería", objPage.atrExpenses().atrItems().get(0).atrDescription());
        assertEquals(bd("15000"), objPage.atrExpenses().atrItems().get(0).atrAmount());
        assertEquals(DATE, objPage.atrExpenses().atrItems().get(0).atrDate());
        assertEquals("Aseo", objPage.atrExpenses().atrItems().get(1).atrDescription());
        assertEquals(bd("35000"), objPage.atrExpenses().atrTotal());
    }

    @Test
    void keepsNullTheValuesThatDoNotExistYet() {
        CashBalanceTableInfo objTable = new CashBalanceTableInfo(DATE, 15L, "OPEN",
                new SavingsFundInfo(bd("100000"), bd("5000"), bd("0"), bd("105000")),
                new ExpensesInfo(List.of(), bd("0")), bd("1500000"), null, null, null);

        CashBalancePage objPage = serviceWith(objTable).getPage(DATE);

        assertNull(objPage.atrDailyCash());
        assertNull(objPage.atrDominusAccumulatedBalance());
        assertNull(objPage.atrDifference());
        assertEquals(bd("1500000"), objPage.atrCashBalance());
        assertEquals(0, objPage.atrExpenses().atrItems().size());
    }

    @Test
    void failsWithNotFoundWhenThereIsNoJournalForTheDate() {
        CashBalancePageService objService = new CashBalancePageService(prmDate -> Optional.empty());

        CashBalanceNotFoundException objException =
                assertThrows(CashBalanceNotFoundException.class, () -> objService.getPage(DATE));

        assertEquals(DATE, objException.getDate());
    }

    @Test
    void rejectsANullDateWithoutAskingTheQuery() {
        int[] arrCalls = {0};
        CashBalanceQuery objQuery = prmDate -> {
            arrCalls[0]++;
            return Optional.empty();
        };

        assertThrows(IllegalArgumentException.class, () -> new CashBalancePageService(objQuery).getPage(null));

        assertEquals(0, arrCalls[0]);
    }

    private static CashBalancePageService serviceWith(CashBalanceTableInfo prmTable) {
        return new CashBalancePageService(prmDate -> Optional.of(prmTable));
    }

    private static CashBalanceTableInfo fullTable() {
        return new CashBalanceTableInfo(DATE, 15L, "CLOSED",
                new SavingsFundInfo(bd("100000"), bd("5000"), bd("0"), bd("105000")),
                new ExpensesInfo(List.of(
                        new ExpenseItemInfo(DATE, "Papelería", bd("15000")),
                        new ExpenseItemInfo(DATE, "Aseo", bd("20000"))), bd("35000")),
                bd("1500000"), bd("250000"), bd("1498000"), bd("2000"));
    }

    private static BigDecimal bd(String prmValue) {
        return new BigDecimal(prmValue);
    }
}
