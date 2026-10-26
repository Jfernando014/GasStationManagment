package com.edu.unicauca.gasstation.backend.sales.application;

import com.edu.unicauca.gasstation.backend.sales.CashBalanceQuery;
import com.edu.unicauca.gasstation.backend.sales.CashBalanceTableInfo;
import com.edu.unicauca.gasstation.backend.sales.CashBalanceTableInfo.ExpenseItemInfo;
import com.edu.unicauca.gasstation.backend.sales.CashBalanceTableInfo.ExpensesInfo;
import com.edu.unicauca.gasstation.backend.sales.CashBalanceTableInfo.SavingsFundInfo;
import com.edu.unicauca.gasstation.backend.sales.domain.JournalSnapshot;
import com.edu.unicauca.gasstation.backend.sales.domain.models.ExpenseFilter;
import com.edu.unicauca.gasstation.backend.sales.domain.models.ExpenseListResult;
import com.edu.unicauca.gasstation.backend.sales.domain.models.SavingsFundDaySummary;
import com.edu.unicauca.gasstation.backend.sales.domain.repositories.JournalDateReadPort;
import com.edu.unicauca.gasstation.backend.sales.domain.services.AdministrativeExpenseService;
import com.edu.unicauca.gasstation.backend.sales.domain.services.CashService;
import com.edu.unicauca.gasstation.backend.sales.domain.services.SavingsFundService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Builds the daily cash balance table for other modules from the journal, the savings fund, the administrative
 * expenses and the cash movements, and returns only the public records of the sales module.
 */
@Service
@Transactional(readOnly = true)
public class CashBalanceQueryService implements CashBalanceQuery {

    private final JournalDateReadPort journals;
    private final SavingsFundService savingsFund;
    private final AdministrativeExpenseService expenses;
    private final CashService cash;

    public CashBalanceQueryService(JournalDateReadPort journals, SavingsFundService savingsFund,
                                   AdministrativeExpenseService expenses, CashService cash) {
        this.journals = journals;
        this.savingsFund = savingsFund;
        this.expenses = expenses;
        this.cash = cash;
    }

    @Override
    public Optional<CashBalanceTableInfo> findDailyCashBalance(LocalDate date) {
        Objects.requireNonNull(date, "date must not be null");
        return journals.findSnapshotByDate(date).map(journal -> build(date, journal));
    }

    private CashBalanceTableInfo build(LocalDate date, JournalSnapshot journal) {
        PendingFields pending = pendingFields(date);
        return new CashBalanceTableInfo(
                date,
                journal.consecutive(),
                journal.status().name(),
                savingsFundOf(date),
                expensesOf(date),
                cash.getBalanceAt(date),
                pending.dailyCash(),
                pending.dominusAccumulatedBalance(),
                pending.difference());
    }

    private SavingsFundInfo savingsFundOf(LocalDate date) {
        SavingsFundDaySummary day = savingsFund.summarize(date);
        return new SavingsFundInfo(day.previousBalance(), day.contribution(), day.payments(),
                day.accumulatedBalance());
    }

    private ExpensesInfo expensesOf(LocalDate date) {
        ExpenseListResult result = expenses.list(new ExpenseFilter(null, null, null, date, null, null));
        return new ExpensesInfo(
                result.expenses().stream()
                        .map(expense -> new ExpenseItemInfo(expense.date(), expense.description(), expense.amount()))
                        .toList(),
                result.total());
    }

    /**
     * The only place to change when the sellers' close (step 2) is published: read the station cash of the day
     * from {@code sellerCloseQuery}, add the Dominus balance once it is stored and calculate the difference.
     * Until then all three values are unavailable.
     */
    private PendingFields pendingFields(LocalDate date) {
        return PendingFields.NOT_AVAILABLE;
    }

    private record PendingFields(BigDecimal dailyCash, BigDecimal dominusAccumulatedBalance, BigDecimal difference) {

        static final PendingFields NOT_AVAILABLE = new PendingFields(null, null, null);
    }
}
