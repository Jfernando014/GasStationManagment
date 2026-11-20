package com.edu.unicauca.gasstation.backend.inventory.domain.services;

import com.edu.unicauca.gasstation.backend.inventory.JournalStepPort;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.StockMovementType;
import com.edu.unicauca.gasstation.backend.inventory.domain.repositories.StockMovementRepository;
import com.edu.unicauca.gasstation.backend.inventory.domain.repositories.TankMeasurementRepository;
import com.edu.unicauca.gasstation.backend.inventory.exception.StockConflictException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Resets what the journal step of tanks and products stores for a date. The opening stock and the manual
 * adjustments are kept: the opening stock is the base of every later day and an adjustment has its own reason.
 */
@Service
@RequiredArgsConstructor
public class InventoryStepResetService {

    static final int STEP_NUMBER = 5;
    private static final List<StockMovementType> STEP_MOVEMENTS = List.of(
            StockMovementType.ENTRY, StockMovementType.TRANSFER, StockMovementType.SALE);

    private final TankMeasurementRepository measurementRepository;
    private final StockMovementRepository movementRepository;
    private final JournalStepPort journalStepPort;

    /** Deletes the tank measurement and the entries, transfers and sales of the date, and returns the step to PENDING. */
    @Transactional
    public void reset(LocalDate journalDate) {
        if (journalStepPort.isJournalClosed(journalDate)) {
            throw new StockConflictException(
                    "La jornada del " + journalDate + " ya está cerrada y no permite modificaciones.");
        }
        measurementRepository.deleteByJournalDate(journalDate);
        movementRepository.deleteByJournalDateAndTypes(journalDate, STEP_MOVEMENTS);
        journalStepPort.returnStepToPending(journalDate, STEP_NUMBER);
    }
}
