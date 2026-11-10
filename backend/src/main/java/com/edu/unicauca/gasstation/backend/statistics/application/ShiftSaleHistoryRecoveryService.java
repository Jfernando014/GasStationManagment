package com.edu.unicauca.gasstation.backend.statistics.application;

import com.edu.unicauca.gasstation.backend.sales.JournalClosedEvent;
import com.edu.unicauca.gasstation.backend.statistics.domain.models.JournalSalesProcessing;
import com.edu.unicauca.gasstation.backend.statistics.domain.repositories.ShiftSaleHistoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.modulith.events.EventPublication;
import org.springframework.modulith.events.IncompleteEventPublications;
import org.springframework.modulith.events.core.TargetEventPublication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Explicit recovery of statistics pending dates and this listener's durable, incomplete publications. */
@Service
@Transactional(propagation = Propagation.NOT_SUPPORTED)
public class ShiftSaleHistoryRecoveryService {
    public static final String LISTENER_ID = "statistics.shift-sales-history";
    private static final Logger LOG = LoggerFactory.getLogger(ShiftSaleHistoryRecoveryService.class);
    private final ShiftSaleHistoryRepository history;
    private final ShiftSaleHistoryRegistrationService registration;
    private final IncompleteEventPublications publications;

    public ShiftSaleHistoryRecoveryService(ShiftSaleHistoryRepository history,
            ShiftSaleHistoryRegistrationService registration, IncompleteEventPublications publications) {
        this.history = history;
        this.registration = registration;
        this.publications = publications;
    }

    /** Also callable explicitly by an operator through the application context; no HTTP endpoint is exposed. */
    public void recoverPending() {
        for (var pending : history.findPending()) {
            try {
                registration.registerClosedJournal(pending.journal());
            } catch (RuntimeException failure) {
                // Registration already keeps the failed attempt pending. Continue with other dates.
                LOG.error("Shift sales remain pending for {}", pending.journal().journalDate(), failure);
            }
        }
        // Replays also close the publication left incomplete before a pending date was successfully recovered.
        // Dates still PENDING have already been attempted in this pass, so do not count another attempt here.
        publications.resubmitIncompletePublications(this::shouldResubmit);
    }

    private boolean shouldResubmit(EventPublication publication) {
        if (!(publication instanceof TargetEventPublication target)
                || !LISTENER_ID.equals(target.getTargetIdentifier().getValue())
                || !(publication.getEvent() instanceof JournalClosedEvent event)) {
            return false;
        }
        return history.findProcessing(event.journalDate())
                .map(state -> state.status() == JournalSalesProcessing.Status.PROCESSED)
                .orElse(true);
    }
}
