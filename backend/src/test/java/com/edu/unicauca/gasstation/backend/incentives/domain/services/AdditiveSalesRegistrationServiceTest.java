package com.edu.unicauca.gasstation.backend.incentives.domain.services;

import static com.edu.unicauca.gasstation.backend.incentives.IncentiveTestData.GASOLINE_NAME;
import static com.edu.unicauca.gasstation.backend.incentives.IncentiveTestData.GASOLINE_SKU;
import static com.edu.unicauca.gasstation.backend.incentives.IncentiveTestData.OCTANE_NAME;
import static com.edu.unicauca.gasstation.backend.incentives.IncentiveTestData.OCTANE_SKU;
import static com.edu.unicauca.gasstation.backend.incentives.IncentiveTestData.clockAt;
import static com.edu.unicauca.gasstation.backend.incentives.IncentiveTestData.sale;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.edu.unicauca.gasstation.backend.incentives.IncentiveTestData.InMemorySales;
import com.edu.unicauca.gasstation.backend.incentives.IncentiveTestData.InMemorySync;
import com.edu.unicauca.gasstation.backend.incentives.domain.models.AdditiveSaleUnits;
import com.edu.unicauca.gasstation.backend.incentives.domain.models.JournalClosure;
import com.edu.unicauca.gasstation.backend.incentives.domain.models.JournalSync;
import com.edu.unicauca.gasstation.backend.incentives.domain.models.SyncStatus;
import com.edu.unicauca.gasstation.backend.incentives.domain.ports.AdditiveSalesPort;
import com.edu.unicauca.gasstation.backend.incentives.domain.repositories.AdditiveSaleRepository;
import com.edu.unicauca.gasstation.backend.incentives.exception.IncentiveError;
import com.edu.unicauca.gasstation.backend.incentives.exception.IncentiveException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Storing the additive units of a closed journal and the state of the date. The transactions (a new one for each
 * operation) belong to Spring and are not tested here; what is checked is what is stored and what is refused.
 */
class AdditiveSalesRegistrationServiceTest {

    private static final LocalDate DATE = LocalDate.of(2026, 4, 18);
    private static final LocalDateTime CLOSED_AT = LocalDateTime.of(2026, 4, 18, 18, 30);
    private static final JournalClosure CLOSING = new JournalClosure(DATE, 42L, 118L, CLOSED_AT);

    private static final String FIRST_INSTANT = "2026-04-18T23:00:00Z";

    private AdditiveSalesPort salesPort;
    private InMemorySales storedSales;
    private InMemorySync sync;
    private AdditiveSalesRegistrationService service;

    @BeforeEach
    void setUp() {
        salesPort = mock(AdditiveSalesPort.class);
        storedSales = new InMemorySales();
        sync = new InMemorySync();
        service = serviceAt(FIRST_INSTANT);
    }

    private AdditiveSalesRegistrationService serviceAt(String instant) {
        Clock clock = clockAt(instant);
        return new AdditiveSalesRegistrationService(salesPort, storedSales, sync, clock);
    }

    private static List<AdditiveSaleUnits> soldOn(LocalDate date) {
        return List.of(
                sale(date, "1001", "ANA MILENA VILLAMIL", GASOLINE_SKU, GASOLINE_NAME, 6),
                sale(date, "1001", "ANA MILENA VILLAMIL", OCTANE_SKU, OCTANE_NAME, 7),
                sale(date, "1002", "CARLOS ANDRÉS PAREDES", GASOLINE_SKU, GASOLINE_NAME, 16));
    }

    // ---- registering a closed journal ----

    @Test
    void theClosedJournalStoresTheUnitsOfItsDateAndMarksTheDateProcessed() {
        when(salesPort.findByDate(DATE)).thenReturn(soldOn(DATE));

        JournalSync result = service.registerClosedJournal(CLOSING);

        assertThat(storedSales.findByDate(DATE)).hasSize(3);
        assertThat(result.status()).isEqualTo(SyncStatus.PROCESSED);
        assertThat(result.attempts()).isEqualTo(1);
        assertThat(result.lastError()).isNull();
        assertThat(result.journalId()).isEqualTo(42L);
        assertThat(result.consecutive()).isEqualTo(118L);
        assertThat(result.closedAt()).isEqualTo(CLOSED_AT);
        assertThat(result.processedAt()).isEqualTo(Instant.parse(FIRST_INSTANT));
        assertThat(sync.findByDate(DATE)).contains(result);
    }

    @Test
    void aDateWhereSalesHoldsNoSellerCloseIsProcessedWithNoRows() {
        when(salesPort.findByDate(DATE)).thenReturn(List.of());

        JournalSync result = service.registerClosedJournal(CLOSING);

        assertThat(storedSales.findByDate(DATE)).isEmpty();
        assertThat(result.status()).isEqualTo(SyncStatus.PROCESSED);
    }

    @Test
    void registeringTheSameClosingAgainReplacesTheRowsInsteadOfDuplicatingThem() {
        when(salesPort.findByDate(DATE)).thenReturn(soldOn(DATE));
        service.registerClosedJournal(CLOSING);
        when(salesPort.findByDate(DATE)).thenReturn(List.of(
                sale(DATE, "1001", "ANA MILENA VILLAMIL", GASOLINE_SKU, GASOLINE_NAME, 9)));

        JournalSync second = service.registerClosedJournal(CLOSING);

        assertThat(storedSales.findByDate(DATE)).singleElement()
                .satisfies(row -> assertThat(row.units()).isEqualTo(9));
        assertThat(second.attempts()).isEqualTo(2);
    }

    @Test
    void otherDatesAreNotTouched() {
        LocalDate earlier = DATE.minusDays(1);
        storedSales.with(soldOn(earlier));
        when(salesPort.findByDate(DATE)).thenReturn(soldOn(DATE));

        service.registerClosedJournal(CLOSING);

        assertThat(storedSales.findByDate(earlier)).hasSize(3);
    }

    @Test
    void ifTheUnitsCannotBeReadNothingIsMarkedAndTheErrorGoesUp() {
        when(salesPort.findByDate(DATE)).thenThrow(new IllegalStateException("sales is down"));

        assertThatThrownBy(() -> service.registerClosedJournal(CLOSING))
                .isInstanceOf(IllegalStateException.class).hasMessage("sales is down");

        assertThat(sync.findByDate(DATE)).isEmpty();
    }

    @Test
    void ifTheRowsCannotBeStoredTheDateIsNotMarkedProcessed() {
        AdditiveSaleRepository failing = mock(AdditiveSaleRepository.class);
        doThrow(new IllegalStateException("disk full")).when(failing).replaceDay(any(), any());
        when(salesPort.findByDate(DATE)).thenReturn(soldOn(DATE));
        AdditiveSalesRegistrationService broken =
                new AdditiveSalesRegistrationService(salesPort, failing, sync, clockAt(FIRST_INSTANT));

        assertThatThrownBy(() -> broken.registerClosedJournal(CLOSING)).isInstanceOf(IllegalStateException.class);

        assertThat(sync.size()).isZero();
    }

    // ---- marking a date pending ----

    @Test
    void aFailureLeavesTheDatePendingWithTheReasonAndTheDataOfTheClosing() {
        JournalSync result = service.markPending(CLOSING, new IllegalStateException("sales is down"));

        assertThat(result.status()).isEqualTo(SyncStatus.PENDING);
        assertThat(result.attempts()).isEqualTo(1);
        assertThat(result.lastError()).isEqualTo("sales is down");
        assertThat(result.processedAt()).isNull();
        assertThat(result.journalId()).isEqualTo(42L);
        assertThat(sync.findPending()).containsExactly(result);
    }

    @Test
    void aSecondFailureCountsTheAttemptAndKeepsTheLastSuccessfulTime() {
        when(salesPort.findByDate(DATE)).thenReturn(soldOn(DATE));
        service.registerClosedJournal(CLOSING);

        JournalSync failed = serviceAt("2026-04-19T10:00:00Z")
                .markPending(CLOSING, new IllegalStateException("falló"));

        assertThat(failed.status()).isEqualTo(SyncStatus.PENDING);
        assertThat(failed.attempts()).isEqualTo(2);
        assertThat(failed.processedAt()).isEqualTo(Instant.parse(FIRST_INSTANT));
        assertThat(failed.updatedAt()).isEqualTo(Instant.parse("2026-04-19T10:00:00Z"));
    }

    @Test
    void aFailureWithoutMessageIsRecordedWithTheNameOfTheException() {
        JournalSync result = service.markPending(CLOSING, new NullPointerException());

        assertThat(result.lastError()).isEqualTo("NullPointerException");
    }

    @Test
    void aBlankMessageIsTreatedAsNoMessage() {
        JournalSync result = service.markPending(CLOSING, new IllegalStateException("   "));

        assertThat(result.lastError()).isEqualTo("IllegalStateException");
    }

    @Test
    void theMessageIsStrippedAndALongOneIsCutTo500Characters() {
        JournalSync stripped = service.markPending(CLOSING, new IllegalStateException("  con espacios  "));
        JournalSync cut = service.markPending(CLOSING, new IllegalStateException("z".repeat(900)));

        assertThat(stripped.lastError()).isEqualTo("con espacios");
        assertThat(cut.lastError()).hasSize(500).endsWith("...");
    }

    // ---- reprocessing ----

    @Test
    void aDateThatNeverHadAClosingCannotBeReprocessedAndTheSalesAreNotRead() {
        IncentiveException error = catchThrowableOfType(() -> service.reprocess(DATE), IncentiveException.class);

        assertThat(error.error()).isEqualTo(IncentiveError.ADDITIVE_SALES_NOT_REGISTERED);
        assertThat(error.getMessage()).contains("2026-04-18");
        verifyNoInteractions(salesPort);
        assertThat(sync.size()).isZero();
        assertThat(storedSales.findByDate(DATE)).isEmpty();
    }

    @Test
    void aPendingDateIsReprocessedWithTheCurrentDataAndBecomesProcessed() {
        service.markPending(CLOSING, new IllegalStateException("sales is down"));
        when(salesPort.findByDate(DATE)).thenReturn(soldOn(DATE));

        JournalSync result = serviceAt("2026-04-19T10:00:00Z").reprocess(DATE);

        assertThat(result.status()).isEqualTo(SyncStatus.PROCESSED);
        assertThat(result.lastError()).isNull();
        assertThat(result.attempts()).isEqualTo(2);
        assertThat(result.processedAt()).isEqualTo(Instant.parse("2026-04-19T10:00:00Z"));
        assertThat(storedSales.findByDate(DATE)).hasSize(3);
        assertThat(sync.findPending()).isEmpty();
    }

    @Test
    void reprocessingKeepsTheJournalDataThatTheClosingBrought() {
        service.markPending(CLOSING, new IllegalStateException("sales is down"));
        when(salesPort.findByDate(DATE)).thenReturn(soldOn(DATE));

        JournalSync result = service.reprocess(DATE);

        assertThat(result.journalId()).isEqualTo(42L);
        assertThat(result.consecutive()).isEqualTo(118L);
        assertThat(result.closedAt()).isEqualTo(CLOSED_AT);
    }

    @Test
    void aProcessedDateCanBeReprocessedAndTheRowsFollowTheCurrentData() {
        when(salesPort.findByDate(DATE)).thenReturn(soldOn(DATE));
        service.registerClosedJournal(CLOSING);
        when(salesPort.findByDate(DATE)).thenReturn(List.of(
                sale(DATE, "1002", "CARLOS ANDRÉS PAREDES", GASOLINE_SKU, GASOLINE_NAME, 20)));

        JournalSync result = service.reprocess(DATE);

        assertThat(result.attempts()).isEqualTo(2);
        assertThat(storedSales.findByDate(DATE)).singleElement()
                .satisfies(row -> assertThat(row.vendorDocument()).isEqualTo("1002"));
    }

    @Test
    void aFailureWhileReprocessingGoesUpAndKeepsWhatWasStoredBefore() {
        when(salesPort.findByDate(DATE)).thenReturn(soldOn(DATE));
        JournalSync processed = service.registerClosedJournal(CLOSING);
        when(salesPort.findByDate(DATE)).thenThrow(new IllegalStateException("sales is down"));

        assertThatThrownBy(() -> service.reprocess(DATE)).isInstanceOf(IllegalStateException.class);

        assertThat(sync.findByDate(DATE)).contains(processed);
        assertThat(storedSales.findByDate(DATE)).hasSize(3);
    }
}
