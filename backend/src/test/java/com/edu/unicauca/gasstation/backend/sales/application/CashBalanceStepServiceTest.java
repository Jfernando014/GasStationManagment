package com.edu.unicauca.gasstation.backend.sales.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.edu.unicauca.gasstation.backend.sales.CashBalanceQuery;
import com.edu.unicauca.gasstation.backend.sales.CashBalanceTableInfo;
import com.edu.unicauca.gasstation.backend.sales.CashBalanceTableInfo.ExpensesInfo;
import com.edu.unicauca.gasstation.backend.sales.CashBalanceTableInfo.SavingsFundInfo;
import com.edu.unicauca.gasstation.backend.sales.application.CashBalanceStepService.Evaluation;
import com.edu.unicauca.gasstation.backend.sales.domain.Journal;
import com.edu.unicauca.gasstation.backend.sales.domain.JournalRepository;
import com.edu.unicauca.gasstation.backend.sales.domain.JournalSnapshot;
import com.edu.unicauca.gasstation.backend.sales.domain.JournalSnapshot.JournalStepSnapshot;
import com.edu.unicauca.gasstation.backend.sales.domain.JournalStatus;
import com.edu.unicauca.gasstation.backend.sales.domain.JournalStep;
import com.edu.unicauca.gasstation.backend.sales.domain.StepStatus;
import com.edu.unicauca.gasstation.backend.sales.domain.models.CashDifferenceStatus;
import com.edu.unicauca.gasstation.backend.sales.domain.models.DominusCashBalance;
import com.edu.unicauca.gasstation.backend.sales.domain.repositories.DominusCashBalanceRepository;
import com.edu.unicauca.gasstation.backend.sales.domain.repositories.JournalDateReadPort;
import com.edu.unicauca.gasstation.backend.sales.domain.services.JournalStepService;
import com.edu.unicauca.gasstation.backend.sales.exception.CashBalanceJournalNotFoundException;
import com.edu.unicauca.gasstation.backend.sales.exception.CashBalanceNotEditableException;
import com.edu.unicauca.gasstation.backend.sales.exception.CashBalanceNotReadyException;
import com.edu.unicauca.gasstation.backend.sales.exception.JournalNotFoundException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CashBalanceStepServiceTest {

    private static final LocalDate DAY = LocalDate.of(2026, 8, 1);
    private static final long JOURNAL_ID = 7L;

    @Mock
    private CashBalanceQuery cashBalanceQuery;
    @Mock
    private JournalDateReadPort journals;
    @Mock
    private JournalRepository journalRepository;
    @Mock
    private DominusCashBalanceRepository dominus;
    @Mock
    private JournalStepService journalStepService;

    private CashBalanceStepService service;

    @BeforeEach
    void setUp() {
        service = new CashBalanceStepService(cashBalanceQuery, journals, journalRepository, dominus,
                journalStepService);
    }

    private static BigDecimal cop(long value) {
        return BigDecimal.valueOf(value);
    }

    /** A table with the four values of step 4; pass null for the ones that do not exist yet. */
    private static CashBalanceTableInfo table(BigDecimal dailyCash, BigDecimal planilla, BigDecimal dominusAccumulated,
                                              BigDecimal difference) {
        return new CashBalanceTableInfo(DAY, 288L, "OPEN",
                new SavingsFundInfo(cop(0), cop(250_000), cop(0), cop(250_000)),
                new ExpensesInfo(List.of(), cop(0)), cop(12_114_000), dailyCash, planilla, dominusAccumulated,
                difference);
    }

    private void givenTable(CashBalanceTableInfo table) {
        when(cashBalanceQuery.findDailyCashBalance(DAY)).thenReturn(Optional.of(table));
    }

    private static JournalSnapshot snapshot(JournalStatus status, StepStatus step4) {
        return new JournalSnapshot(JOURNAL_ID, DAY, 288L, status, LocalDateTime.of(2026, 8, 1, 7, 0),
                List.of(new JournalStepSnapshot(4, "Saldo en caja", step4, null)));
    }

    private void givenJournalWithId() {
        when(journalRepository.findById(JOURNAL_ID)).thenReturn(Optional.of(Journal.builder().id(JOURNAL_ID)
                .date(DAY).consecutive(288L).status(JournalStatus.OPEN).build()));
    }

    // ------------------------------------------------------------------ evaluate

    @Test
    void whenEverythingIsAvailableTheStepCanBeCompletedAndThePlanillaIsReportedAsHigher() {
        givenTable(table(cop(12_516_000), cop(24_630_000), cop(24_629_469), cop(-531)));

        Evaluation evaluation = service.evaluate(DAY);

        assertThat(evaluation.canComplete()).isTrue();
        assertThat(evaluation.missing()).isEmpty();
        assertThat(evaluation.differenceStatus()).isEqualTo(CashDifferenceStatus.PLANILLA_MAYOR);
        assertThat(evaluation.table().difference()).isEqualByComparingTo("-531");
    }

    @Test
    void aMatchingDayIsCuadra() {
        givenTable(table(cop(1_000), cop(5_000), cop(5_000), cop(0)));

        assertThat(service.evaluate(DAY).differenceStatus()).isEqualTo(CashDifferenceStatus.CUADRA);
    }

    @Test
    void aPositiveDifferenceMeansThePlanillaHasLess() {
        givenTable(table(cop(1_000), cop(5_000), cop(5_500), cop(500)));

        assertThat(service.evaluate(DAY).differenceStatus()).isEqualTo(CashDifferenceStatus.PLANILLA_MENOR);
    }

    @Test
    void withoutTheSellersCloseTheStepCannotBeCompleted() {
        givenTable(table(null, null, cop(24_629_469), null));

        Evaluation evaluation = service.evaluate(DAY);

        assertThat(evaluation.canComplete()).isFalse();
        assertThat(evaluation.differenceStatus()).isNull();
        assertThat(evaluation.missing()).hasSize(1);
        assertThat(evaluation.missing().get(0)).contains("paso 2");
    }

    @Test
    void withoutTheInitialCashBalanceTheStepCannotBeCompleted() {
        givenTable(table(cop(12_516_000), null, cop(24_629_469), null));

        Evaluation evaluation = service.evaluate(DAY);

        assertThat(evaluation.canComplete()).isFalse();
        assertThat(evaluation.missing()).hasSize(1);
        assertThat(evaluation.missing().get(0)).contains("saldo inicial");
    }

    @Test
    void withoutTheDominusBalanceTheStepCannotBeCompleted() {
        givenTable(table(cop(12_516_000), cop(24_630_000), null, null));

        Evaluation evaluation = service.evaluate(DAY);

        assertThat(evaluation.canComplete()).isFalse();
        assertThat(evaluation.missing()).hasSize(1);
        assertThat(evaluation.missing().get(0)).contains("Dominus");
    }

    @Test
    void withNoSourceTheSellersCloseAndTheDominusBalanceAreListed() {
        givenTable(table(null, null, null, null));

        assertThat(service.evaluate(DAY).missing()).hasSize(2);
    }

    @Test
    void aDateWithoutJournalIsNotFound() {
        when(cashBalanceQuery.findDailyCashBalance(DAY)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.evaluate(DAY)).isInstanceOf(CashBalanceJournalNotFoundException.class);
    }

    // ------------------------------------------------------------------- save

    @Test
    void savingStoresTheFiguresAndReturnsTheTableRecalculated() {
        when(journals.findSnapshotByDate(DAY)).thenReturn(Optional.of(snapshot(JournalStatus.OPEN, StepStatus.PENDING)));
        givenTable(table(cop(12_516_000), cop(24_630_000), cop(24_629_469), cop(-531)));

        Evaluation evaluation = service.saveDominusBalance(DAY, cop(24_629_469));

        ArgumentCaptor<DominusCashBalance> saved = ArgumentCaptor.forClass(DominusCashBalance.class);
        verify(dominus).save(saved.capture());
        assertThat(saved.getValue().date()).isEqualTo(DAY);
        assertThat(saved.getValue().accumulatedBalance()).isEqualByComparingTo("24629469");
        assertThat(evaluation.table().difference()).isEqualByComparingTo("-531");
    }

    @Test
    void savingOnADateWithoutJournalIsNotFound() {
        when(journals.findSnapshotByDate(DAY)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.saveDominusBalance(DAY, cop(1)))
                .isInstanceOf(CashBalanceJournalNotFoundException.class);
        verify(dominus, never()).save(any());
    }

    @Test
    void savingOnAClosedJournalIsRejected() {
        when(journals.findSnapshotByDate(DAY)).thenReturn(Optional.of(snapshot(JournalStatus.CLOSED, StepStatus.PENDING)));

        assertThatThrownBy(() -> service.saveDominusBalance(DAY, cop(1)))
                .isInstanceOf(CashBalanceNotEditableException.class)
                .hasMessageContaining("cerrada");
        verify(dominus, never()).save(any());
    }

    @Test
    void savingOnceStepFourIsCompletedIsRejected() {
        when(journals.findSnapshotByDate(DAY)).thenReturn(Optional.of(snapshot(JournalStatus.OPEN, StepStatus.COMPLETED)));

        assertThatThrownBy(() -> service.saveDominusBalance(DAY, cop(1)))
                .isInstanceOf(CashBalanceNotEditableException.class)
                .hasMessageContaining("paso 4");
        verify(dominus, never()).save(any());
    }

    // --------------------------------------------------------------- complete

    @Test
    void completesStepFourEvenWithAShortfall() {
        givenJournalWithId();
        givenTable(table(cop(12_516_000), cop(24_630_000), cop(24_629_469), cop(-531)));
        JournalStep completed = JournalStep.builder().id(40L).stepNumber(4).name("Saldo en caja")
                .status(StepStatus.COMPLETED).build();
        when(journalStepService.markStepAsCompleted(JOURNAL_ID, 4)).thenReturn(completed);

        JournalStep result = service.complete(JOURNAL_ID);

        assertThat(result).isSameAs(completed);
        verify(journalStepService).markStepAsCompleted(JOURNAL_ID, 4);
    }

    @Test
    void doesNotCompleteStepFourWhileSomethingIsMissing() {
        givenJournalWithId();
        givenTable(table(null, null, null, null));

        assertThatThrownBy(() -> service.complete(JOURNAL_ID))
                .isInstanceOfSatisfying(CashBalanceNotReadyException.class,
                        ex -> assertThat(ex.getMissing()).hasSize(2));
        verify(journalStepService, never()).markStepAsCompleted(any(), any());
    }

    @Test
    void completingAnUnknownJournalIsNotFound() {
        when(journalRepository.findById(JOURNAL_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.complete(JOURNAL_ID)).isInstanceOf(JournalNotFoundException.class);
        verify(journalStepService, never()).markStepAsCompleted(any(), any());
    }

    @Test
    void theRequirementOfStepsTwoAndThreeIsStillCheckedByTheJournalStepService() {
        givenJournalWithId();
        givenTable(table(cop(1), cop(1), cop(1), cop(0)));
        when(journalStepService.markStepAsCompleted(JOURNAL_ID, 4))
                .thenThrow(new IllegalStateException("No se puede avanzar: el paso 3 no está completado."));

        assertThatThrownBy(() -> service.complete(JOURNAL_ID))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("paso 3");
    }
}
