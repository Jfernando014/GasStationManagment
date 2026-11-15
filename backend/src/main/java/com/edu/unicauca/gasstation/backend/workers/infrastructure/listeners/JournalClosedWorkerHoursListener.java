package com.edu.unicauca.gasstation.backend.workers.infrastructure.listeners;

import com.edu.unicauca.gasstation.backend.sales.JournalClosedEvent;
import com.edu.unicauca.gasstation.backend.workers.domain.models.JournalClosure;
import com.edu.unicauca.gasstation.backend.workers.domain.services.WorkerHoursRegistrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/** Keeps the hours worked by each worker when a journal is closed. */
@Slf4j
@Component
@RequiredArgsConstructor
public class JournalClosedWorkerHoursListener {

    private final WorkerHoursRegistrationService registration;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onJournalClosed(JournalClosedEvent event) {
        JournalClosure closure = new JournalClosure(event.journalDate(), event.journalId(), event.consecutive(),
                event.closedAt());
        try {
            registration.registerClosedJournal(closure);
        } catch (RuntimeException ex) {
            // The journal is already closed; a failure here must not turn the closing into an error. The date is
            // left pending so it can be reprocessed.
            log.error("Could not keep the worked hours of {}", event.journalDate(), ex);
            markPending(closure, ex);
        }
    }

    private void markPending(JournalClosure closure, RuntimeException cause) {
        try {
            registration.markPending(closure, cause);
        } catch (RuntimeException ex) {
            log.error("Could not mark the worked hours of {} as pending", closure.workDate(), ex);
        }
    }
}
