package com.edu.unicauca.gasstation.backend.sales.application;

import com.edu.unicauca.gasstation.backend.sales.CashBalanceQuery;
import com.edu.unicauca.gasstation.backend.sales.CashBalanceTableInfo;
import com.edu.unicauca.gasstation.backend.sales.CashBalanceTableInfo.ExpenseItemInfo;
import com.edu.unicauca.gasstation.backend.sales.CashBalanceTableInfo.ExpensesInfo;
import com.edu.unicauca.gasstation.backend.sales.CashBalanceTableInfo.SavingsFundInfo;
import com.edu.unicauca.gasstation.backend.sales.SellerCloseQuery;
import com.edu.unicauca.gasstation.backend.sales.domain.JournalSnapshot;
import com.edu.unicauca.gasstation.backend.sales.domain.models.CashMovement;
import com.edu.unicauca.gasstation.backend.sales.domain.models.CashMovementType;
import com.edu.unicauca.gasstation.backend.sales.domain.models.DominusCashBalance;
import com.edu.unicauca.gasstation.backend.sales.domain.models.ExpenseFilter;
import com.edu.unicauca.gasstation.backend.sales.domain.models.ExpenseListResult;
import com.edu.unicauca.gasstation.backend.sales.domain.models.SavingsFundDaySummary;
import com.edu.unicauca.gasstation.backend.sales.domain.repositories.DominusCashBalanceRepository;
import com.edu.unicauca.gasstation.backend.sales.domain.repositories.JournalDateReadPort;
import com.edu.unicauca.gasstation.backend.sales.domain.services.AdministrativeExpenseService;
import com.edu.unicauca.gasstation.backend.sales.domain.services.CashBalanceCalculator;
import com.edu.unicauca.gasstation.backend.sales.domain.services.CashService;
import com.edu.unicauca.gasstation.backend.sales.domain.services.SavingsFundService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
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
    private final SellerCloseQuery sellerCloses;
    private final DominusCashBalanceRepository dominus;

    public CashBalanceQueryService(JournalDateReadPort journals, SavingsFundService savingsFund,
                                   AdministrativeExpenseService expenses, CashService cash,
                                   SellerCloseQuery sellerCloses, DominusCashBalanceRepository dominus) {
        this.journals = journals;
        this.savingsFund = savingsFund;
        this.expenses = expenses;
        this.cash = cash;
        this.sellerCloses = sellerCloses;
        this.dominus = dominus;
    }

    @Override
    public Optional<CashBalanceTableInfo> findDailyCashBalance(LocalDate date) {
        Objects.requireNonNull(date, "date must not be null");
        return journals.findSnapshotByDate(date).map(journal -> build(date, journal));
    }

    private CashBalanceTableInfo build(LocalDate date, JournalSnapshot journal) {
        BigDecimal cashMovementsBalance = cash.getBalanceAt(date);
        DominusComparison comparison = compareWithDominus(date, cashMovementsBalance);
        return new CashBalanceTableInfo(
                date,
                journal.consecutive(),
                journal.status().name(),
                savingsFundOf(date),
                expensesOf(date),
                cashMovementsBalance,
                comparison.dailyCash(),
                comparison.planillaCashBalance(),
                comparison.dominusAccumulatedBalance(),
                comparison.difference());
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
     * Station cash of the day, the "saldo de efectivo en planilla de ventas" and the difference against the Dominus
     * accumulated balance (see {@link CashBalanceCalculator}).
     *
     * <p>The planilla balance is the cash movements balance plus the station cash of every day from the opening day
     * (the date of the initial balance movement) up to the date. Each value is {@code null} while its source is
     * missing: no seller close for the date, no initial balance registered (or the date is before it), or no Dominus
     * balance stored.
     */
    private DominusComparison compareWithDominus(LocalDate date, BigDecimal cashMovementsBalance) {
        BigDecimal dailyCash = stationCashOf(date).orElse(null);
        BigDecimal planilla = dailyCash == null ? null : planillaBalance(date, dailyCash, cashMovementsBalance);
        Optional<DominusCashBalance> stored = dominus.findByDate(date);
        BigDecimal difference = planilla != null && stored.isPresent()
                ? CashBalanceCalculator.difference(stored.get().accumulatedBalance(), planilla)
                : null;
        return new DominusComparison(dailyCash, planilla,
                stored.map(DominusCashBalance::accumulatedBalance).orElse(null), difference);
    }

    private BigDecimal planillaBalance(LocalDate date, BigDecimal dailyCash, BigDecimal cashMovementsBalance) {
        Optional<LocalDate> opening = cash.listMovements().stream()
                .filter(movement -> movement.getMovementType() == CashMovementType.INITIAL_BALANCE)
                .map(CashMovement::getMovementDate)
                .min(Comparator.naturalOrder());
        if (opening.isEmpty() || date.isBefore(opening.get())) {
            return null;
        }
        BigDecimal accumulated = dailyCash;
        for (LocalDate day = opening.get(); day.isBefore(date); day = day.plusDays(1)) {
            accumulated = accumulated.add(stationCashOf(day).orElse(BigDecimal.ZERO));
        }
        return CashBalanceCalculator.planillaBalance(cashMovementsBalance, accumulated);
    }

    private Optional<BigDecimal> stationCashOf(LocalDate date) {
        return sellerCloses.findDaySummary(date).map(SellerCloseQuery.DaySummary::totalCash);
    }

    private record DominusComparison(BigDecimal dailyCash, BigDecimal planillaCashBalance,
                                     BigDecimal dominusAccumulatedBalance, BigDecimal difference) {
    }
}
