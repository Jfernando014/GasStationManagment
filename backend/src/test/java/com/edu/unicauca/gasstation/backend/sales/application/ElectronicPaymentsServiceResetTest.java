package com.edu.unicauca.gasstation.backend.sales.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.edu.unicauca.gasstation.backend.sales.SellerCloseQuery;
import com.edu.unicauca.gasstation.backend.sales.domain.Journal;
import com.edu.unicauca.gasstation.backend.sales.domain.JournalRepository;
import com.edu.unicauca.gasstation.backend.sales.domain.JournalStatus;
import com.edu.unicauca.gasstation.backend.sales.domain.JournalStep;
import com.edu.unicauca.gasstation.backend.sales.domain.StepStatus;
import com.edu.unicauca.gasstation.backend.sales.domain.repositories.CardReconciliationRepository;
import com.edu.unicauca.gasstation.backend.shifts.spi.ScheduleWorkerDirectory;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Reset of step 3 (vouchers, Redeban rows and Puntos Colombia total) of an open journal date. */
@ExtendWith(MockitoExtension.class)
class ElectronicPaymentsServiceResetTest {

    private static final LocalDate DATE = LocalDate.of(2026, 8, 5);

    @Mock
    private CredibancoReconciliationService credibancoService;
    @Mock
    private QrPointsReconciliationService qrPointsService;
    @Mock
    private CardReconciliationRepository cards;
    @Mock
    private SellerCloseQuery sellerCloseQuery;
    @Mock
    private JournalRepository journals;
    @Mock
    private ScheduleWorkerDirectory workers;

    private ElectronicPaymentsService service;

    @BeforeEach
    void setUp() {
        service = new ElectronicPaymentsService(
                credibancoService, qrPointsService, cards, sellerCloseQuery, journals, workers);
    }

    private static JournalStep step(int number, StepStatus status) {
        return JournalStep.builder().stepNumber(number).name("Paso " + number).status(status)
                .completedAt(status == StepStatus.COMPLETED ? LocalDateTime.of(2026, 8, 5, 19, 0) : null).build();
    }

    private static Journal journal(JournalStatus status, JournalStep... steps) {
        return Journal.builder().id(9L).date(DATE).consecutive(290L).status(status)
                .steps(new ArrayList<>(List.of(steps))).build();
    }

    @Test
    void resettingWithoutADateDoesNothing() {
        service.reset(null);

        verifyNoInteractions(journals, cards, qrPointsService);
    }

    @Test
    void anOpenJournalLosesTheVouchersTheRedebanRowsAndThePointsTotalAndStepThreeGoesBackToPending() {
        Journal journal = journal(JournalStatus.OPEN, step(2, StepStatus.COMPLETED), step(3, StepStatus.COMPLETED));
        when(journals.findByDate(DATE)).thenReturn(Optional.of(journal));

        service.reset(DATE);

        JournalStep step3 = journal.getSteps().get(1);
        assertThat(step3.getStatus()).isEqualTo(StepStatus.PENDING);
        assertThat(step3.getCompletedAt()).isNull();
        verify(journals).save(journal);
        verify(cards).deleteByDate(DATE);
        verify(qrPointsService).removeDay(DATE);
    }

    @Test
    void theJournalIsSavedBeforeTheDataIsDeleted() {
        Journal journal = journal(JournalStatus.OPEN, step(3, StepStatus.COMPLETED));
        when(journals.findByDate(DATE)).thenReturn(Optional.of(journal));

        service.reset(DATE);

        InOrder order = inOrder(journals, cards, qrPointsService);
        order.verify(journals).save(journal);
        order.verify(cards).deleteByDate(DATE);
        order.verify(qrPointsService).removeDay(DATE);
    }

    @Test
    void theOtherStepsKeepTheirState() {
        Journal journal = journal(JournalStatus.OPEN, step(2, StepStatus.COMPLETED), step(3, StepStatus.COMPLETED),
                step(4, StepStatus.COMPLETED));
        when(journals.findByDate(DATE)).thenReturn(Optional.of(journal));

        service.reset(DATE);

        assertThat(journal.getSteps()).extracting(JournalStep::getStatus)
                .containsExactly(StepStatus.COMPLETED, StepStatus.PENDING, StepStatus.COMPLETED);
    }

    @Test
    void aStepThreeThatWasNeverCompletedStaysPendingAndTheDataIsStillDeleted() {
        Journal journal = journal(JournalStatus.OPEN, step(2, StepStatus.COMPLETED), step(3, StepStatus.PENDING));
        when(journals.findByDate(DATE)).thenReturn(Optional.of(journal));

        service.reset(DATE);

        assertThat(journal.getSteps().get(1).getStatus()).isEqualTo(StepStatus.PENDING);
        verify(cards).deleteByDate(DATE);
        verify(qrPointsService).removeDay(DATE);
    }

    @Test
    void aJournalWithoutStepThreeIsNotSavedButTheDataIsDeleted() {
        when(journals.findByDate(DATE)).thenReturn(Optional.of(journal(JournalStatus.OPEN, step(2, StepStatus.COMPLETED))));

        service.reset(DATE);

        verify(journals, never()).save(any());
        verify(cards).deleteByDate(DATE);
    }

    @Test
    void aDateWithoutJournalOnlyDeletesTheData() {
        when(journals.findByDate(DATE)).thenReturn(Optional.empty());

        service.reset(DATE);

        verify(journals, never()).save(any());
        verify(cards).deleteByDate(DATE);
        verify(qrPointsService).removeDay(DATE);
    }

    @Test
    void aClosedJournalCannotBeResetAndNothingIsDeleted() {
        when(journals.findByDate(DATE)).thenReturn(Optional.of(
                journal(JournalStatus.CLOSED, step(3, StepStatus.COMPLETED))));

        assertThatThrownBy(() -> service.reset(DATE))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("La jornada del 2026-08-05 ya está cerrada y no permite modificaciones.");

        verify(journals, never()).save(any());
        verifyNoInteractions(cards, qrPointsService);
    }

    @Test
    void anOpenJournalWhoseClosingStepIsCompletedCannotBeResetEither() {
        when(journals.findByDate(DATE)).thenReturn(Optional.of(
                journal(JournalStatus.OPEN, step(3, StepStatus.COMPLETED), step(6, StepStatus.COMPLETED))));

        assertThatThrownBy(() -> service.reset(DATE)).isInstanceOf(IllegalStateException.class);

        verifyNoInteractions(cards, qrPointsService);
    }
}
