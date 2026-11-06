package com.edu.unicauca.gasstation.backend.inventory.infrastructure.listeners;

import com.edu.unicauca.gasstation.backend.inventory.domain.events.TankMeasurementSavedEvent;
import com.edu.unicauca.gasstation.backend.inventory.domain.repositories.TankMeasurementRepository;
import com.edu.unicauca.gasstation.backend.inventory.domain.services.InventoryAlertService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDate;

@Slf4j
@Component
@RequiredArgsConstructor
public class InventoryAlertListener {

    private static final LocalDate FAR_FUTURE = LocalDate.of(9999, 12, 31);

    private final InventoryAlertService alertService;
    private final TankMeasurementRepository measurementRepository;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onMeasurementSaved(TankMeasurementSavedEvent event) {
        try {
            // Alerts describe the current state: editing an old journal must not overwrite it
            if (isLatest(event.journalDate())) {
                alertService.reconcile(event.journalDate());
            }
        } catch (RuntimeException ex) {
            // The measurement is already committed; a failing alert must not turn it into an error
            log.error("Could not reconcile the inventory alerts for {}", event.journalDate(), ex);
        }
    }

    private boolean isLatest(LocalDate journalDate) {
        return measurementRepository.findLatestBefore(FAR_FUTURE)
                .map(latest -> latest.getJournalDate().equals(journalDate))
                .orElse(false);
    }
}