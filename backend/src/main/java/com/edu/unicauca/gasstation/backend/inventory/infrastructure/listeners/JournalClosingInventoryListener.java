package com.edu.unicauca.gasstation.backend.inventory.infrastructure.listeners;

import com.edu.unicauca.gasstation.backend.inventory.JournalClosingNotification;
import com.edu.unicauca.gasstation.backend.inventory.domain.repositories.TankMeasurementRepository;
import com.edu.unicauca.gasstation.backend.inventory.domain.services.InventoryAlertService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDate;

/**
 * Reconciles the inventory autonomy alerts when the operational journal is closed (S3-19).
 *
 * <p>It listens to {@link JournalClosingNotification}, the consumer-owned contract implemented by
 * {@code sales.JournalClosedEvent}, so {@code inventory} never imports {@code sales}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JournalClosingInventoryListener {

    private static final LocalDate FAR_FUTURE = LocalDate.of(9999, 12, 31);

    private final InventoryAlertService alertService;
    private final TankMeasurementRepository measurementRepository;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onJournalClosed(JournalClosingNotification notification) {
        try {
            // Alerts describe the current state: closing an old journal must not overwrite it
            if (isLatest(notification.journalDate())) {
                alertService.reconcile(notification.journalDate());
            }
        } catch (RuntimeException ex) {
            // The journal is already closed; a failing alert must not turn it into an error
            log.error("Could not reconcile the inventory alerts for {}", notification.journalDate(), ex);
        }
    }

    private boolean isLatest(LocalDate journalDate) {
        return measurementRepository.findLatestBefore(FAR_FUTURE)
                .map(latest -> latest.getJournalDate().equals(journalDate))
                .orElse(false);
    }
}
