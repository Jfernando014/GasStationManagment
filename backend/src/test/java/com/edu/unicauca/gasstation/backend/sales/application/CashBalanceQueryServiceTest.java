package com.edu.unicauca.gasstation.backend.sales.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.edu.unicauca.gasstation.backend.sales.CashBalanceTableInfo;
import com.edu.unicauca.gasstation.backend.sales.SellerCloseQuery;
import com.edu.unicauca.gasstation.backend.sales.domain.JournalSnapshot;
import com.edu.unicauca.gasstation.backend.sales.domain.JournalStatus;
import com.edu.unicauca.gasstation.backend.sales.domain.models.AdministrativeExpense;
import com.edu.unicauca.gasstation.backend.sales.domain.models.CashMovement;
import com.edu.unicauca.gasstation.backend.sales.domain.models.CashMovementType;
import com.edu.unicauca.gasstation.backend.sales.domain.models.DominusCashBalance;
import com.edu.unicauca.gasstation.backend.sales.domain.models.SavingsFundDaySummary;
import com.edu.unicauca.gasstation.backend.sales.domain.repositories.AdministrativeExpenseRepository;
import com.edu.unicauca.gasstation.backend.sales.domain.repositories.DominusCashBalanceRepository;
import com.edu.unicauca.gasstation.backend.sales.domain.repositories.JournalDateReadPort;
import com.edu.unicauca.gasstation.backend.sales.domain.services.AdministrativeExpenseService;
import com.edu.unicauca.gasstation.backend.sales.domain.services.CashService;
import com.edu.unicauca.gasstation.backend.sales.domain.services.SavingsFundService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
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
    @Mock
    private SellerCloseQuery sellerCloses;
    @Mock
    private DominusCashBalanceRepository dominus;

    private CashBalanceQueryService service;

    @BeforeEach
    void setUp() {
        service = new CashBalanceQueryService(journals, savingsFund,
                new AdministrativeExpenseService(expenseRepository), cash, sellerCloses, dominus);
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

    /** Same empty day as {@link #givenDay}, for any date and with the cash movements balance of that date. */
    private void givenDayOn(LocalDate date, long cashMovementsBalance) {
        when(journals.findSnapshotByDate(date)).thenReturn(Optional.of(new JournalSnapshot(
                7L, date, 1L, JournalStatus.OPEN, date.atTime(7, 0), List.of())));
        when(savingsFund.summarize(date)).thenReturn(noFundMovement());
        when(cash.getBalanceAt(date)).thenReturn(cop(cashMovementsBalance));
        when(expenseRepository.findBetween(date, date)).thenReturn(List.of());
    }

    private static CashMovement movement(CashMovementType type, LocalDate date, long amount) {
        return new CashMovement(1L, type, date, cop(amount), null);
    }

    /** The day total of the sellers' close: only the cash matters to the cash balance table. */
    private static SellerCloseQuery.DaySummary daySummary(LocalDate date, long totalCash) {
        BigDecimal zero = BigDecimal.ZERO;
        return new SellerCloseQuery.DaySummary(date, cop(totalCash), zero, zero, zero, zero, zero, zero, zero,
                zero, zero, zero, zero, zero, zero, 0, List.of());
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
    void withoutSellerCloseNorDominusBalanceTheFourValuesAreNull() {
        givenDay(noFundMovement(), 0, List.of());

        CashBalanceTableInfo table = service.findDailyCashBalance(DAY).orElseThrow();

        assertThat(table.dailyCash()).isNull();
        assertThat(table.planillaCashBalance()).isNull();
        assertThat(table.dominusAccumulatedBalance()).isNull();
        assertThat(table.difference()).isNull();
    }

    @Test
    void withoutAnInitialBalanceThereIsNoPlanillaBalance() {
        givenDay(noFundMovement(), 0, List.of());
        when(sellerCloses.findDaySummary(DAY)).thenReturn(Optional.of(daySummary(DAY, 1_000_000)));
        when(dominus.findByDate(DAY)).thenReturn(Optional.of(new DominusCashBalance(DAY, cop(5_000_000))));

        CashBalanceTableInfo table = service.findDailyCashBalance(DAY).orElseThrow();

        assertThat(table.dailyCash()).isEqualByComparingTo("1000000");
        assertThat(table.planillaCashBalance()).isNull();
        assertThat(table.dominusAccumulatedBalance()).isEqualByComparingTo("5000000");
        assertThat(table.difference()).isNull();
    }

    @Test
    void theDayOfTheInitialBalanceAddsItsStationCashToThePlanillaBalance() {
        givenDay(noFundMovement(), 2_000_000, List.of());
        when(cash.listMovements()).thenReturn(List.of(movement(CashMovementType.INITIAL_BALANCE, DAY, 2_000_000)));
        when(sellerCloses.findDaySummary(DAY)).thenReturn(Optional.of(daySummary(DAY, 500_000)));

        CashBalanceTableInfo table = service.findDailyCashBalance(DAY).orElseThrow();

        assertThat(table.planillaCashBalance()).isEqualByComparingTo("2500000");
        assertThat(table.dominusAccumulatedBalance()).isNull();
        assertThat(table.difference()).isNull();
    }

    @Test
    void withoutTheSellersCloseThereIsNoPlanillaBalanceEvenWithAnInitialBalance() {
        givenDay(noFundMovement(), 2_000_000, List.of());
        when(dominus.findByDate(DAY)).thenReturn(Optional.of(new DominusCashBalance(DAY, cop(5_000_000))));

        CashBalanceTableInfo table = service.findDailyCashBalance(DAY).orElseThrow();

        assertThat(table.dailyCash()).isNull();
        assertThat(table.planillaCashBalance()).isNull();
        assertThat(table.difference()).isNull();
    }

    @Test
    void aDominusBalanceHigherThanThePlanillaGivesAPositiveDifference() {
        givenDay(noFundMovement(), 2_000_000, List.of());
        when(cash.listMovements()).thenReturn(List.of(movement(CashMovementType.INITIAL_BALANCE, DAY, 2_000_000)));
        when(sellerCloses.findDaySummary(DAY)).thenReturn(Optional.of(daySummary(DAY, 500_000)));
        when(dominus.findByDate(DAY)).thenReturn(Optional.of(new DominusCashBalance(DAY, cop(2_500_074))));

        assertThat(service.findDailyCashBalance(DAY).orElseThrow().difference()).isEqualByComparingTo("74");
    }

    /**
     * 01/08/2026, as the Excel "Saldo en caja" computes it. The Dominus balance is the real one from the cash
     * complement of the repository (src/test/resources/dominus/cash-complement.json). The initial balance is the
     * planilla balance before the day: 24.630.000 - 12.516.000 = 12.114.000.
     */
    @Test
    void differenceOfAugustFirstIsMinus531() throws IOException {
        LocalDate firstOfAugust = LocalDate.of(2026, 8, 1);
        JsonNode complement;
        try (InputStream json = getClass().getResourceAsStream("/dominus/cash-complement.json")) {
            assertThat(json).as("fixture dominus/cash-complement.json").isNotNull();
            complement = new ObjectMapper().readTree(json);
        }
        assertThat(complement.at("/header/reportDate").asText()).isEqualTo("2026-08-01");
        BigDecimal dominusAccumulated = complement.at("/accumulatedCash/accumulatedBalance").decimalValue();
        assertThat(dominusAccumulated).isEqualByComparingTo("24629469");

        givenDayOn(firstOfAugust, 12_114_000);
        when(cash.listMovements()).thenReturn(
                List.of(movement(CashMovementType.INITIAL_BALANCE, firstOfAugust, 12_114_000)));
        when(sellerCloses.findDaySummary(firstOfAugust)).thenReturn(Optional.of(daySummary(firstOfAugust, 12_516_000)));
        when(dominus.findByDate(firstOfAugust)).thenReturn(
                Optional.of(new DominusCashBalance(firstOfAugust, dominusAccumulated)));

        CashBalanceTableInfo table = service.findDailyCashBalance(firstOfAugust).orElseThrow();

        assertThat(table.dailyCash()).isEqualByComparingTo("12516000");
        assertThat(table.planillaCashBalance()).isEqualByComparingTo("24630000");
        assertThat(table.dominusAccumulatedBalance()).isEqualByComparingTo("24629469");
        assertThat(table.difference()).isEqualByComparingTo("-531");
    }

    @Test
    void thePlanillaBalanceAccumulatesTheStationCashOfEveryDaySinceTheOpening() {
        LocalDate first = LocalDate.of(2026, 8, 1);
        LocalDate second = LocalDate.of(2026, 8, 2);
        // movements balance at the 2nd: initial 12.114.000 less a deposit of 1.000.000
        givenDayOn(second, 11_114_000);
        when(cash.listMovements()).thenReturn(List.of(
                movement(CashMovementType.DEPOSIT, second, 1_000_000),
                movement(CashMovementType.INITIAL_BALANCE, first, 12_114_000)));
        when(sellerCloses.findDaySummary(first)).thenReturn(Optional.of(daySummary(first, 12_516_000)));
        when(sellerCloses.findDaySummary(second)).thenReturn(Optional.of(daySummary(second, 3_000_000)));
        when(dominus.findByDate(second)).thenReturn(Optional.of(new DominusCashBalance(second, cop(26_630_000))));

        CashBalanceTableInfo table = service.findDailyCashBalance(second).orElseThrow();

        assertThat(table.planillaCashBalance()).isEqualByComparingTo("26630000");
        assertThat(table.difference()).isEqualByComparingTo("0");
    }

    @Test
    void aDayWithoutSellerCloseInTheMiddleCountsAsZero() {
        LocalDate first = LocalDate.of(2026, 8, 1);
        LocalDate third = LocalDate.of(2026, 8, 3);
        givenDayOn(third, 1_000_000);
        when(cash.listMovements()).thenReturn(List.of(movement(CashMovementType.INITIAL_BALANCE, first, 1_000_000)));
        when(sellerCloses.findDaySummary(first)).thenReturn(Optional.of(daySummary(first, 100_000)));
        when(sellerCloses.findDaySummary(third)).thenReturn(Optional.of(daySummary(third, 300_000)));

        assertThat(service.findDailyCashBalance(third).orElseThrow().planillaCashBalance())
                .isEqualByComparingTo("1400000");
    }

    @Test
    void aDateBeforeTheOpeningDayHasNoPlanillaBalance() {
        LocalDate opening = LocalDate.of(2026, 8, 5);
        LocalDate before = LocalDate.of(2026, 8, 3);
        givenDayOn(before, 0);
        when(cash.listMovements()).thenReturn(List.of(movement(CashMovementType.INITIAL_BALANCE, opening, 1_000_000)));
        when(sellerCloses.findDaySummary(before)).thenReturn(Optional.of(daySummary(before, 100_000)));

        assertThat(service.findDailyCashBalance(before).orElseThrow().planillaCashBalance()).isNull();
    }

    @Test
    void rejectsANullDate() {
        assertThatThrownBy(() -> service.findDailyCashBalance(null)).isInstanceOf(NullPointerException.class);
    }
}
