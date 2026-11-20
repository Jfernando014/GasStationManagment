package com.edu.unicauca.gasstation.backend.incentives.infrastructure.listeners;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import com.edu.unicauca.gasstation.backend.incentives.domain.models.JournalClosure;
import com.edu.unicauca.gasstation.backend.incentives.domain.services.AdditiveSalesRegistrationService;
import com.edu.unicauca.gasstation.backend.sales.JournalClosedEvent;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** The listener must never throw: the journal is already closed and committed when it runs. */
class JournalClosedAdditiveSalesListenerTest {

    private static final LocalDate DATE = LocalDate.of(2026, 4, 18);
    private static final LocalDateTime CLOSED_AT = LocalDateTime.of(2026, 4, 18, 18, 30);
    private static final JournalClosedEvent EVENT = new JournalClosedEvent(42L, DATE, 118L, CLOSED_AT);
    private static final JournalClosure CLOSURE = new JournalClosure(DATE, 42L, 118L, CLOSED_AT);

    private AdditiveSalesRegistrationService registration;
    private JournalClosedAdditiveSalesListener listener;

    @BeforeEach
    void setUp() {
        registration = mock(AdditiveSalesRegistrationService.class);
        listener = new JournalClosedAdditiveSalesListener(registration);
    }

    @Test
    void aClosedJournalIsRegisteredWithTheDataOfTheEvent() {
        listener.onJournalClosed(EVENT);

        verify(registration).registerClosedJournal(CLOSURE);
        verifyNoMoreInteractions(registration);
    }

    @Test
    void whenTheRegistrationFailsTheDateIsMarkedPendingWithTheSameClosingAndTheCause() {
        IllegalStateException cause = new IllegalStateException("sales is down");
        doThrow(cause).when(registration).registerClosedJournal(CLOSURE);

        assertThatCode(() -> listener.onJournalClosed(EVENT)).doesNotThrowAnyException();

        verify(registration).markPending(CLOSURE, cause);
    }

    @Test
    void whenMarkingThePendingStateAlsoFailsTheListenerStillDoesNotThrow() {
        doThrow(new IllegalStateException("sales is down")).when(registration).registerClosedJournal(CLOSURE);
        doThrow(new IllegalStateException("db is down")).when(registration).markPending(any(), any());

        assertThatCode(() -> listener.onJournalClosed(EVENT)).doesNotThrowAnyException();
    }

    @Test
    void aSuccessfulRegistrationNeverMarksPending() {
        listener.onJournalClosed(EVENT);

        verify(registration, never()).markPending(any(), any());
    }

    @Test
    void anEventWithoutJournalIdIsRegisteredAnyway() {
        JournalClosedEvent withoutId = new JournalClosedEvent(null, DATE, 118L, CLOSED_AT);

        listener.onJournalClosed(withoutId);

        verify(registration).registerClosedJournal(new JournalClosure(DATE, null, 118L, CLOSED_AT));
    }
}
