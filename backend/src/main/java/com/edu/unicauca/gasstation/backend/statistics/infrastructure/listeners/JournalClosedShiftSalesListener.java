package com.edu.unicauca.gasstation.backend.statistics.infrastructure.listeners;

import com.edu.unicauca.gasstation.backend.sales.JournalClosedEvent;
import com.edu.unicauca.gasstation.backend.statistics.application.ShiftSaleHistoryRegistrationService;
import com.edu.unicauca.gasstation.backend.statistics.application.ShiftSaleHistoryRecoveryService;
import com.edu.unicauca.gasstation.backend.statistics.domain.models.ClosedJournalSnapshot;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/** Receives committed journal closings. A failure must leave Modulith's durable publication incomplete. */
@Component
public class JournalClosedShiftSalesListener {
    public static final String LISTENER_ID = ShiftSaleHistoryRecoveryService.LISTENER_ID;

    private final ShiftSaleHistoryRegistrationService registration;

    public JournalClosedShiftSalesListener(ShiftSaleHistoryRegistrationService registration) {
        this.registration = registration;
    }

    @TransactionalEventListener(id = LISTENER_ID, phase = TransactionPhase.AFTER_COMMIT)
    public void onJournalClosed(JournalClosedEvent event) {
        // The service/repository record PENDING themselves. Do not double-count failures or swallow exceptions:
        // if PENDING cannot be written, the publication persisted with the closing remains recoverable.
        registration.registerClosedJournal(new ClosedJournalSnapshot(
                event.journalId(), event.journalDate(), event.consecutive(), event.closedAt()));
    }
}
