package com.edu.unicauca.gasstation.backend.statistics.application;

import com.edu.unicauca.gasstation.backend.sales.JournalClosedEvent;
import com.edu.unicauca.gasstation.backend.statistics.domain.models.ClosedJournalSnapshot;
import com.edu.unicauca.gasstation.backend.statistics.domain.models.JournalSalesProcessing;
import com.edu.unicauca.gasstation.backend.statistics.domain.repositories.ShiftSaleHistoryRepository;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.modulith.events.EventPublication;
import org.springframework.modulith.events.IncompleteEventPublications;
import org.springframework.modulith.events.core.PublicationTargetIdentifier;
import org.springframework.modulith.events.core.TargetEventPublication;

import static com.edu.unicauca.gasstation.backend.statistics.ShiftSaleRegistrationTestData.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class ShiftSaleHistoryRecoveryServiceTest {
    private final ShiftSaleHistoryRepository history = mock(ShiftSaleHistoryRepository.class);
    private final ShiftSaleHistoryRegistrationService registration = mock(ShiftSaleHistoryRegistrationService.class);
    private final IncompleteEventPublications publications = mock(IncompleteEventPublications.class);
    private final ShiftSaleHistoryRecoveryService recovery =
            new ShiftSaleHistoryRecoveryService(history, registration, publications);

    @BeforeEach
    void noPendingByDefault() {
        when(history.findPending()).thenReturn(List.of());
    }

    @Test
    void explicitlyRetriesTheStoredOriginalJournal() {
        when(history.findPending()).thenReturn(List.of(pending()));

        recovery.recoverPending();

        verify(registration).registerClosedJournal(JOURNAL);
        verify(history, never()).markPending(any(), anyString());
        verify(publications).resubmitIncompletePublications(any());
    }

    @Test
    void aFailedDateDoesNotPreventOtherPendingDatesFromRecovering() {
        var next = new ClosedJournalSnapshot(16L, DATE.plusDays(1), 106L, JOURNAL.closedAt().plusDays(1));
        var secondPending = new JournalSalesProcessing(next, JournalSalesProcessing.Status.PENDING,
                1, null, "source unavailable", null, pending().updatedAt());
        when(history.findPending()).thenReturn(List.of(pending(), secondPending));
        when(registration.registerClosedJournal(JOURNAL)).thenThrow(new IllegalStateException("still offline"));

        recovery.recoverPending();

        verify(registration).registerClosedJournal(JOURNAL);
        verify(registration).registerClosedJournal(next);
        verify(publications).resubmitIncompletePublications(any());
    }

    @Test
    void completesItsOwnPublicationWhenPendingRecoveryAlreadySucceeded() {
        when(history.findProcessing(DATE)).thenReturn(Optional.of(processed(1)));

        assertThat(replayFilter().test(publication(ShiftSaleHistoryRecoveryService.LISTENER_ID, event()))).isTrue();
    }

    @Test
    void replaysItsDurablePublicationWhenEvenThePendingMarkerCouldNotBeWritten() {
        when(history.findProcessing(DATE)).thenReturn(Optional.empty());

        assertThat(replayFilter().test(publication(ShiftSaleHistoryRecoveryService.LISTENER_ID, event()))).isTrue();
    }

    @Test
    void doesNotAttemptAFailedPendingDateTwiceDuringTheSamePass() {
        when(history.findProcessing(DATE)).thenReturn(Optional.of(pending()));

        assertThat(replayFilter().test(publication(ShiftSaleHistoryRecoveryService.LISTENER_ID, event()))).isFalse();
    }

    @Test
    void doesNotReplayAnotherConsumersPublications() {
        assertThat(replayFilter().test(publication("incentives.another-listener", event()))).isFalse();
        verify(history, never()).findProcessing(any());
    }

    @Test
    void rejectsUnrelatedEventTypesAndPublicationsWithoutATarget() {
        var filter = replayFilter();

        assertThat(filter.test(publication(ShiftSaleHistoryRecoveryService.LISTENER_ID, "unrelated"))).isFalse();
        assertThat(filter.test(mock(EventPublication.class))).isFalse();
        verify(history, never()).findProcessing(any());
    }

    @Test
    void aDatabaseOutageIsVisibleToTheScheduledBoundaryForALaterPass() {
        when(history.findPending()).thenThrow(new IllegalStateException("database offline"));

        assertThatThrownBy(recovery::recoverPending).hasMessage("database offline");
        verifyNoInteractions(registration, publications);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private Predicate<EventPublication> replayFilter() {
        recovery.recoverPending();
        ArgumentCaptor<Predicate> captor = ArgumentCaptor.forClass(Predicate.class);
        verify(publications).resubmitIncompletePublications(captor.capture());
        return captor.getValue();
    }

    private static EventPublication publication(String listener, Object event) {
        return TargetEventPublication.of(event, PublicationTargetIdentifier.of(listener));
    }

    private static JournalClosedEvent event() {
        return new JournalClosedEvent(JOURNAL.journalId(), DATE, JOURNAL.consecutive(), JOURNAL.closedAt());
    }
}