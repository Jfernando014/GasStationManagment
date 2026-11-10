package com.edu.unicauca.gasstation.backend.incentives.infrastructure.listeners;

import com.edu.unicauca.gasstation.backend.incentives.domain.models.JournalClosure;
import com.edu.unicauca.gasstation.backend.incentives.domain.services.AdditiveSalesRegistrationService;
import com.edu.unicauca.gasstation.backend.sales.JournalClosedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/** Stores the additive units sold per seller when a journal is closed. */
@Slf4j
@Component
@RequiredArgsConstructor
public class JournalClosedAdditiveSalesListener {

    private final AdditiveSalesRegistrationService registration;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onJournalClosed(JournalClosedEvent event) {
        JournalClosure closure = new JournalClosure(event.journalDate(), event.journalId(), event.consecutive(),
                event.closedAt());
        try {
            registration.registerClosedJournal(closure);
        } catch (RuntimeException ex) {
            // The journal is already closed; a failure here must not turn the closing into an error. The date is
            // left pending so it can be reprocessed.
            log.error("Could not store the additive sales of {}", event.journalDate(), ex);
            markPending(closure, ex);
        }
    }

    private void markPending(JournalClosure closure, RuntimeException cause) {
        try {
            registration.markPending(closure, cause);
        } catch (RuntimeException ex) {
            log.error("Could not mark the additive sales of {} as pending", closure.journalDate(), ex);
        }
    }
}
