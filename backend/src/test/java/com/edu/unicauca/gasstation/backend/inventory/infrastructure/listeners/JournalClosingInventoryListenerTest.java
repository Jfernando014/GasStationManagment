package com.edu.unicauca.gasstation.backend.inventory.infrastructure.listeners;

import com.edu.unicauca.gasstation.backend.inventory.JournalClosingNotification;
import com.edu.unicauca.gasstation.backend.inventory.domain.repositories.TankMeasurementRepository;
import com.edu.unicauca.gasstation.backend.inventory.domain.services.InventoryAlertService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static com.edu.unicauca.gasstation.backend.inventory.MeasurementTestData.measurement;
import static com.edu.unicauca.gasstation.backend.inventory.MeasurementTestData.reading;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JournalClosingInventoryListenerTest {

    private static final LocalDate DATE = LocalDate.of(2026, 8, 5);
    private static final JournalClosingNotification NOTIFICATION = () -> DATE;

    @Mock
    private InventoryAlertService alertService;
    @Mock
    private TankMeasurementRepository measurementRepository;

    private JournalClosingInventoryListener listener;

    @BeforeEach
    void setUp() {
        listener = new JournalClosingInventoryListener(alertService, measurementRepository);
    }

    @Test
    void reconcilesWhenTheClosedJournalIsTheLatestOne() {
        givenLatestMeasurementOn(DATE);

        listener.onJournalClosed(NOTIFICATION);

        verify(alertService).reconcile(DATE);
    }

    @Test
    void ignoresTheCloseOfAnOlderJournal() {
        givenLatestMeasurementOn(DATE.plusDays(1));

        listener.onJournalClosed(NOTIFICATION);

        verify(alertService, never()).reconcile(any());
    }

    @Test
    void ignoresTheEventWhenThereIsNoMeasurement() {
        when(measurementRepository.findLatestBefore(any(LocalDate.class))).thenReturn(Optional.empty());

        listener.onJournalClosed(NOTIFICATION);

        verify(alertService, never()).reconcile(any());
    }

    @Test
    void aFailingReconciliationNeverReachesTheCaller() {
        givenLatestMeasurementOn(DATE);
        doThrow(new IllegalStateException("boom")).when(alertService).reconcile(DATE);

        assertThatCode(() -> listener.onJournalClosed(NOTIFICATION)).doesNotThrowAnyException();
    }

    private void givenLatestMeasurementOn(LocalDate date) {
        when(measurementRepository.findLatestBefore(any(LocalDate.class)))
                .thenReturn(Optional.of(measurement(1L, date, reading(1L, "55", "539"))));
    }
}
