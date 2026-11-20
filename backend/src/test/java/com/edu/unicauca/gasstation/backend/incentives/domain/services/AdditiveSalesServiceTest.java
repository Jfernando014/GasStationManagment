package com.edu.unicauca.gasstation.backend.incentives.domain.services;

import static com.edu.unicauca.gasstation.backend.incentives.IncentiveTestData.GASOLINE_NAME;
import static com.edu.unicauca.gasstation.backend.incentives.IncentiveTestData.GASOLINE_SKU;
import static com.edu.unicauca.gasstation.backend.incentives.IncentiveTestData.sale;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.edu.unicauca.gasstation.backend.incentives.IncentiveTestData.InMemorySales;
import com.edu.unicauca.gasstation.backend.incentives.IncentiveTestData.InMemorySync;
import com.edu.unicauca.gasstation.backend.incentives.domain.models.AdditiveSaleUnits;
import com.edu.unicauca.gasstation.backend.incentives.domain.models.AdditiveSalesDay;
import com.edu.unicauca.gasstation.backend.incentives.domain.models.JournalClosure;
import com.edu.unicauca.gasstation.backend.incentives.domain.models.JournalSync;
import com.edu.unicauca.gasstation.backend.incentives.domain.models.SyncStatus;
import com.edu.unicauca.gasstation.backend.incentives.exception.IncentiveError;
import com.edu.unicauca.gasstation.backend.incentives.exception.IncentiveException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Reading the stored additive sales and reprocessing a date by hand. */
class AdditiveSalesServiceTest {

    private static final LocalDate DATE = LocalDate.of(2026, 4, 18);
    private static final Instant NOW = Instant.parse("2026-04-19T10:00:00Z");
    private static final JournalClosure CLOSING = new JournalClosure(DATE, 42L, 118L,
            LocalDateTime.of(2026, 4, 18, 18, 30));

    private AdditiveSalesRegistrationService registration;
    private InMemorySales storedSales;
    private InMemorySync sync;
    private AdditiveSalesService service;

    @BeforeEach
    void setUp() {
        registration = mock(AdditiveSalesRegistrationService.class);
        storedSales = new InMemorySales();
        sync = new InMemorySync();
        service = new AdditiveSalesService(registration, storedSales, sync);
    }

    private static JournalSync processed() {
        return JournalSync.processed(null, CLOSING, NOW);
    }

    private static IncentiveException failureOf(org.assertj.core.api.ThrowableAssert.ThrowingCallable call) {
        return catchThrowableOfType(call, IncentiveException.class);
    }

    // ---- reading ----

    @Test
    void aDateWithAClosingShowsItsStateAndItsRows() {
        AdditiveSaleUnits row = sale(DATE, "1001", "ANA MILENA VILLAMIL", GASOLINE_SKU, GASOLINE_NAME, 6);
        storedSales.with(row);
        sync.with(processed());

        AdditiveSalesDay day = service.getDay(DATE);

        assertThat(day.sync().status()).isEqualTo(SyncStatus.PROCESSED);
        assertThat(day.sales()).containsExactly(row);
    }

    @Test
    void aDateWithoutAClosingIsNotRegistered() {
        IncentiveException error = failureOf(() -> service.getDay(DATE));

        assertThat(error.error()).isEqualTo(IncentiveError.ADDITIVE_SALES_NOT_REGISTERED);
        assertThat(error.getMessage()).contains("2026-04-18");
    }

    @Test
    void aProcessedDateWithoutRowsIsStillRegistered() {
        sync.with(processed());

        assertThat(service.getDay(DATE).sales()).isEmpty();
    }

    @Test
    void thePendingDatesAreTheOnesThatFailed() {
        JournalSync pending = JournalSync.pending(null, CLOSING, "falló", NOW);
        sync.with(pending).with(JournalSync.processed(null,
                JournalClosure.ofDate(DATE.minusDays(5)), NOW));

        assertThat(service.listPending()).containsExactly(pending);
    }

    // ---- reprocessing ----

    @Test
    void reprocessingReturnsTheDateAsItWasStored() {
        sync.with(processed());
        storedSales.with(sale(DATE, "1001", "ANA MILENA VILLAMIL", GASOLINE_SKU, GASOLINE_NAME, 6));

        AdditiveSalesDay day = service.reprocess(DATE);

        verify(registration).reprocess(DATE);
        assertThat(day.sales()).hasSize(1);
        assertThat(day.sync().status()).isEqualTo(SyncStatus.PROCESSED);
    }

    @Test
    void ifReprocessingFailsTheDateIsLeftPendingAndTheAnswerIsAnIncentiveError() {
        sync.with(processed());
        IllegalStateException cause = new IllegalStateException("sales is down");
        when(registration.reprocess(DATE)).thenThrow(cause);

        IncentiveException error = failureOf(() -> service.reprocess(DATE));

        assertThat(error.error()).isEqualTo(IncentiveError.ADDITIVE_SALES_PROCESSING_FAILED);
        assertThat(error.getMessage()).contains("2026-04-18").contains("pendiente");
        verify(registration).markPending(eq(JournalClosure.ofDate(DATE)), eq(cause));
    }

    @Test
    void theErrorOfMarkingThePendingStateDoesNotHideTheOriginalFailure() {
        when(registration.reprocess(DATE)).thenThrow(new IllegalStateException("sales is down"));
        doThrow(new IllegalStateException("db is down too")).when(registration).markPending(any(), any());

        IncentiveException error = failureOf(() -> service.reprocess(DATE));

        assertThat(error.error()).isEqualTo(IncentiveError.ADDITIVE_SALES_PROCESSING_FAILED);
    }

    @Test
    void aDateThatWasNeverClosedIsRefusedAsItIsAndIsNotMarkedPending() {
        IncentiveException refused = new IncentiveException(IncentiveError.ADDITIVE_SALES_NOT_REGISTERED, DATE);
        when(registration.reprocess(DATE)).thenThrow(refused);

        IncentiveException error = failureOf(() -> service.reprocess(DATE));

        assertThat(error).isSameAs(refused);
        verify(registration, never()).markPending(any(), any());
    }
}
