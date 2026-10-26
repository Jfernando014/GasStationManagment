package com.edu.unicauca.gasstation.backend.sales.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.edu.unicauca.gasstation.backend.sales.CashBalanceTableInfo;
import com.edu.unicauca.gasstation.backend.sales.domain.JournalSnapshot;
import com.edu.unicauca.gasstation.backend.sales.domain.JournalStatus;
import com.edu.unicauca.gasstation.backend.sales.domain.models.AdministrativeExpense;
import com.edu.unicauca.gasstation.backend.sales.domain.models.SavingsFundDaySummary;
import com.edu.unicauca.gasstation.backend.sales.domain.repositories.AdministrativeExpenseRepository;
import com.edu.unicauca.gasstation.backend.sales.domain.repositories.JournalDateReadPort;
import com.edu.unicauca.gasstation.backend.sales.domain.services.AdministrativeExpenseService;
import com.edu.unicauca.gasstation.backend.sales.domain.services.CashService;
import com.edu.unicauca.gasstation.backend.sales.domain.services.SavingsFundService;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CashBalanceQueryServiceTest {

    private static final LocalDate DAY = LocalDate.of(2026, 8, 5);

    @Mock
    private JournalDateReadPort journals;
    @Mock
    private SavingsFundService savingsFund;
    @Mock
    private CashService cash;
    @Mock
    private AdministrativeExpenseRepository expenseRepository;

    private CashBalanceQueryService service;

    @BeforeEach
    void setUp() {
        service = new CashBalanceQueryService(journals, savingsFund,
                new AdministrativeExpenseService(expenseRepository), cash);
    }

    private static BigDecimal cop(long value) {
        return BigDecimal.valueOf(value);
    }

    private static AdministrativeExpense expense(long amount, String description) {
        return new AdministrativeExpense(1L, DAY, cop(amount), description, Instant.parse("2026-08-05T15:00:00Z"));
    }

    /** Stubs a day with a journal so the whole table can be built. */
    private void givenDay(SavingsFundDaySummary fund, long cashBalance, List<AdministrativeExpense> expenses) {
        when(journals.findSnapshotByDate(DAY)).thenReturn(Optional.of(new JournalSnapshot(
                7L, DAY, 288L, JournalStatus.OPEN, LocalDateTime.of(2026, 8, 5, 7, 0), List.of())));
        when(savingsFund.summarize(DAY)).thenReturn(fund);
        when(cash.getBalanceAt(DAY)).thenReturn(cop(cashBalance));
        when(expenseRepository.findBetween(DAY, DAY)).thenReturn(expenses);
    }

    private static SavingsFundDaySummary noFundMovement() {
        return new SavingsFundDaySummary(cop(0), cop(0), cop(0));
    }

    @Test
    void returnsEmptyWhenTheDateHasNoJournal() {
        when(journals.findSnapshotByDate(DAY)).thenReturn(Optional.empty());

        assertThat(service.findDailyCashBalance(DAY)).isEmpty();
        verifyNoInteractions(savingsFund, cash, expenseRepository);
    }

    @Test
    void publishesDateConsecutiveAndStatusOfTheJournal() {
        givenDay(noFundMovement(), 0, List.of());

        CashBalanceTableInfo table = service.findDailyCashBalance(DAY).orElseThrow();

        assertThat(table.date()).isEqualTo(DAY);
        assertThat(table.journalConsecutive()).isEqualTo(288L);
        assertThat(table.journalStatus()).isEqualTo("OPEN");
    }

    @Test
    void rebuildsTheSavingsFundOfTheDay() {
        givenDay(new SavingsFundDaySummary(cop(3_750_000), cop(250_000), cop(0)), 0, List.of());

        CashBalanceTableInfo.SavingsFundInfo fund = service.findDailyCashBalance(DAY).orElseThrow().savingsFund();

        assertThat(fund.previousBalance()).isEqualByComparingTo("3750000");
        assertThat(fund.contribution()).isEqualByComparingTo("250000");
        assertThat(fund.payments()).isEqualByComparingTo("0");
        assertThat(fund.accumulatedBalance()).isEqualByComparingTo("4000000");
    }

    @Test
    void aPaymentOfTheDayLowersTheAccumulatedFund() {
        givenDay(new SavingsFundDaySummary(cop(4_000_000), cop(250_000), cop(1_000_000)), 0, List.of());

        CashBalanceTableInfo.SavingsFundInfo fund = service.findDailyCashBalance(DAY).orElseThrow().savingsFund();

        assertThat(fund.payments()).isEqualByComparingTo("1000000");
        assertThat(fund.accumulatedBalance()).isEqualByComparingTo("3250000");
    }

    @Test
    void listsOnlyTheExpensesOfTheDateWithTheirTotal() {
        givenDay(noFundMovement(), 0, List.of(expense(45_000, "Papelería"), expense(30_000, "Vale transporte")));

        CashBalanceTableInfo.ExpensesInfo expenses = service.findDailyCashBalance(DAY).orElseThrow().expenses();

        verify(expenseRepository).findBetween(DAY, DAY);
        assertThat(expenses.items()).hasSize(2);
        assertThat(expenses.items().get(0).description()).isEqualTo("Papelería");
        assertThat(expenses.items().get(0).amount()).isEqualByComparingTo("45000");
        assertThat(expenses.total()).isEqualByComparingTo("75000");
    }

    @Test
    void aDayWithoutExpensesGivesAnEmptyListAndZeroTotal() {
        givenDay(noFundMovement(), 0, List.of());

        CashBalanceTableInfo.ExpensesInfo expenses = service.findDailyCashBalance(DAY).orElseThrow().expenses();

        assertThat(expenses.items()).isEmpty();
        assertThat(expenses.total()).isEqualByComparingTo("0");
    }

    @Test
    void takesTheCashBalanceFromTheCashService() {
        givenDay(noFundMovement(), 1_000_000, List.of());

        assertThat(service.findDailyCashBalance(DAY).orElseThrow().cashBalance()).isEqualByComparingTo("1000000");
    }

    @Test
    void valuesWithoutSourceAreNullUntilStepTwoIsPublished() {
        givenDay(noFundMovement(), 0, List.of());

        CashBalanceTableInfo table = service.findDailyCashBalance(DAY).orElseThrow();

        assertThat(table.dailyCash()).isNull();
        assertThat(table.dominusAccumulatedBalance()).isNull();
        assertThat(table.difference()).isNull();
    }

    @Test
    void rejectsANullDate() {
        assertThatThrownBy(() -> service.findDailyCashBalance(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    @Disabled("Enable when sellerCloseQuery (step 2) is published and CashBalanceQueryService.pendingFields uses it")
    void differenceOfAugustFirstIsMinus531() {
        fail("Pending: build the 01/08 fixture from the Excel and assert difference = -531");
    }
}
