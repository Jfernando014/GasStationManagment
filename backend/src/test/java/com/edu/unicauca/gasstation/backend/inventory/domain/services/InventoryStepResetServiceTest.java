package com.edu.unicauca.gasstation.backend.inventory.domain.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.edu.unicauca.gasstation.backend.inventory.JournalStepPort;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.StockMovementType;
import com.edu.unicauca.gasstation.backend.inventory.domain.repositories.StockMovementRepository;
import com.edu.unicauca.gasstation.backend.inventory.domain.repositories.TankMeasurementRepository;
import com.edu.unicauca.gasstation.backend.inventory.exception.StockConflictException;
import java.time.LocalDate;
import java.util.Collection;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Reset of step 5: what is deleted, what is kept and when the reset is refused. */
@ExtendWith(MockitoExtension.class)
class InventoryStepResetServiceTest {

    private static final LocalDate DAY = LocalDate.of(2026, 8, 5);

    @Mock
    private TankMeasurementRepository measurementRepository;
    @Mock
    private StockMovementRepository movementRepository;
    @Mock
    private JournalStepPort journalStepPort;

    @InjectMocks
    private InventoryStepResetService service;

    @Test
    void anOpenJournalLosesItsMeasurementAndItsMovementsAndStepFiveGoesBackToPending() {
        when(journalStepPort.isJournalClosed(DAY)).thenReturn(false);

        service.reset(DAY);

        verify(measurementRepository).deleteByJournalDate(DAY);
        verify(movementRepository).deleteByJournalDateAndTypes(eq(DAY), anyCollection());
        verify(journalStepPort).returnStepToPending(DAY, 5);
    }

    @Test
    @SuppressWarnings("unchecked")
    void onlyEntriesTransfersAndSalesAreDeletedNeverTheOpeningStockNorTheAdjustments() {
        service.reset(DAY);

        ArgumentCaptor<Collection<StockMovementType>> types = ArgumentCaptor.forClass(Collection.class);
        verify(movementRepository).deleteByJournalDateAndTypes(eq(DAY), types.capture());
        assertThat(types.getValue()).containsExactlyInAnyOrder(
                StockMovementType.ENTRY, StockMovementType.TRANSFER, StockMovementType.SALE);
        assertThat(types.getValue()).doesNotContain(StockMovementType.INITIAL, StockMovementType.ADJUSTMENT);
    }

    @Test
    void theStepReturnedToPendingIsNumberFive() {
        assertThat(InventoryStepResetService.STEP_NUMBER).isEqualTo(5);
    }

    @Test
    void theClosedCheckComesFirstAndTheStepIsReturnedToPendingAfterTheDeletes() {
        service.reset(DAY);

        InOrder order = inOrder(journalStepPort, measurementRepository, movementRepository);
        order.verify(journalStepPort).isJournalClosed(DAY);
        order.verify(measurementRepository).deleteByJournalDate(DAY);
        order.verify(movementRepository).deleteByJournalDateAndTypes(eq(DAY), anyCollection());
        order.verify(journalStepPort).returnStepToPending(DAY, 5);
    }

    @Test
    void aClosedJournalIsRefusedWithAConflictAndNothingIsDeleted() {
        when(journalStepPort.isJournalClosed(DAY)).thenReturn(true);

        assertThatThrownBy(() -> service.reset(DAY))
                .isInstanceOf(StockConflictException.class)
                .hasMessage("La jornada del 2026-08-05 ya está cerrada y no permite modificaciones.");

        verifyNoInteractions(measurementRepository, movementRepository);
        verify(journalStepPort, never()).returnStepToPending(any(), anyInt());
    }

    @Test
    void ifDeletingFailsTheStepIsNotReturnedToPendingAndTheErrorGoesUp() {
        IllegalStateException failure = new IllegalStateException("database down");
        doThrow(failure).when(movementRepository).deleteByJournalDateAndTypes(eq(DAY), anyCollection());

        assertThatThrownBy(() -> service.reset(DAY)).isSameAs(failure);

        verify(journalStepPort, never()).returnStepToPending(any(), anyInt());
    }
}
