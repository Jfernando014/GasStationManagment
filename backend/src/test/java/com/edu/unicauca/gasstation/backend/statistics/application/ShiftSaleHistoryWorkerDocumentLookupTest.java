package com.edu.unicauca.gasstation.backend.statistics.application;

import static com.edu.unicauca.gasstation.backend.statistics.ShiftSaleRegistrationTestData.DATE;
import static com.edu.unicauca.gasstation.backend.statistics.ShiftSaleRegistrationTestData.JOURNAL;
import static com.edu.unicauca.gasstation.backend.statistics.ShiftSaleRegistrationTestData.WORKER_A;
import static com.edu.unicauca.gasstation.backend.statistics.ShiftSaleRegistrationTestData.WORKER_B;
import static com.edu.unicauca.gasstation.backend.statistics.ShiftSaleRegistrationTestData.close;
import static com.edu.unicauca.gasstation.backend.statistics.ShiftSaleRegistrationTestData.day;
import static com.edu.unicauca.gasstation.backend.statistics.ShiftSaleRegistrationTestData.member;
import static com.edu.unicauca.gasstation.backend.statistics.ShiftSaleRegistrationTestData.pending;
import static com.edu.unicauca.gasstation.backend.statistics.ShiftSaleRegistrationTestData.processed;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.edu.unicauca.gasstation.backend.sales.SellerCloseQuery;
import com.edu.unicauca.gasstation.backend.shifts.daystaff.DayStaff;
import com.edu.unicauca.gasstation.backend.shifts.daystaff.DayStaffQuery;
import com.edu.unicauca.gasstation.backend.shifts.spi.ScheduleWorkerDirectory;
import com.edu.unicauca.gasstation.backend.statistics.domain.models.ShiftSaleHistory;
import com.edu.unicauca.gasstation.backend.statistics.domain.repositories.ShiftSaleHistoryRepository;
import com.edu.unicauca.gasstation.backend.statistics.exception.ShiftSaleHistoryIntegrationException;
import com.edu.unicauca.gasstation.backend.statistics.exception.ShiftSaleHistoryIntegrationException.Code;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;

/**
 * How the vendor of a seller close is identified: a full worker UUID is used as it is, and any other identifier
 * (a cédula or a legacy placeholder) is looked up as the document of a registered worker.
 */
class ShiftSaleHistoryWorkerDocumentLookupTest {

    private SellerCloseQuery sales;
    private DayStaffQuery staff;
    private ShiftSaleHistoryRepository history;
    private ScheduleWorkerDirectory workers;
    private ShiftSaleHistoryRegistrationService service;

    @BeforeEach
    void setUp() {
        sales = mock(SellerCloseQuery.class);
        staff = mock(DayStaffQuery.class);
        history = mock(ShiftSaleHistoryRepository.class);
        workers = mock(ScheduleWorkerDirectory.class);
        service = new ShiftSaleHistoryRegistrationService(sales, staff, history, workers);
    }

    private void givenCloses(String... documents) {
        when(sales.findVendorClosesByDate(DATE)).thenReturn(
                java.util.Arrays.stream(documents).map(document -> close(document, "DIA")).toList());
    }

    private void givenStaff(DayStaff.Member... members) {
        when(staff.getDayStaff(DATE)).thenReturn(day(members));
    }

    private void persist() {
        when(history.saveJournal(eq(JOURNAL), anyList())).thenAnswer(invocation ->
                processed(invocation.<List<ShiftSaleHistory>>getArgument(1).size()));
    }

    @SuppressWarnings("unchecked")
    private List<ShiftSaleHistory> saved() {
        ArgumentCaptor<List<ShiftSaleHistory>> captor = ArgumentCaptor.forClass(List.class);
        verify(history).saveJournal(eq(JOURNAL), captor.capture());
        return captor.getValue();
    }

    private ShiftSaleHistoryIntegrationException rejected() {
        when(history.markPending(eq(JOURNAL), anyString())).thenReturn(pending());
        ShiftSaleHistoryIntegrationException[] caught = new ShiftSaleHistoryIntegrationException[1];
        assertThatThrownBy(() -> service.registerClosedJournal(JOURNAL))
                .isInstanceOfSatisfying(ShiftSaleHistoryIntegrationException.class, failure -> caught[0] = failure);
        verify(history, never()).saveJournal(any(), anyList());
        return caught[0];
    }

    // ---- a document of a registered worker ----

    @Test
    void aCedulaOfARegisteredWorkerIdentifiesTheVendor() {
        givenCloses("001001");
        givenStaff(member(WORKER_A, "DIA", "TITULAR"));
        when(workers.findWorkerIdByDocument("001001")).thenReturn(Optional.of(WORKER_A));
        persist();

        var result = service.registerClosedJournal(JOURNAL);

        assertThat(result.saleCount()).isEqualTo(1);
        ShiftSaleHistory snapshot = saved().getFirst();
        assertThat(snapshot.workerId()).isEqualTo(WORKER_A);
        assertThat(snapshot.workedCode()).isEqualTo("DIA");
        // The history keeps the identifier exactly as sales reported it
        assertThat(snapshot.identity().vendorDocument()).isEqualTo("001001");
    }

    @Test
    void theDocumentIsStrippedBeforeTheLookupButKeptAsReportedInTheIdentity() {
        givenCloses(" 001001 ");
        givenStaff(member(WORKER_A, "DIA", "TITULAR"));
        when(workers.findWorkerIdByDocument("001001")).thenReturn(Optional.of(WORKER_A));
        persist();

        service.registerClosedJournal(JOURNAL);

        verify(workers).findWorkerIdByDocument("001001");
        assertThat(saved().getFirst().workerId()).isEqualTo(WORKER_A);
    }

    @Test
    void aVendorByCedulaIsStillMatchedAgainstTheDayStaffByWorkerAndShift() {
        givenCloses("001001");
        givenStaff(member(WORKER_B, "DIA", "TITULAR"));
        when(workers.findWorkerIdByDocument("001001")).thenReturn(Optional.of(WORKER_A));

        assertThat(rejected().code()).isEqualTo(Code.WORKER_NOT_IN_DAY_STAFF);
    }

    @Test
    void sellersIdentifiedByUuidAndByCedulaCanComeInTheSameJournal() {
        when(sales.findVendorClosesByDate(DATE)).thenReturn(List.of(
                close(WORKER_A, "DIA"), close("002002", "NOCHE")));
        givenStaff(member(WORKER_A, "DIA", "TITULAR"), member(WORKER_B, "NOCHE", "TITULAR"));
        when(workers.findWorkerIdByDocument("002002")).thenReturn(Optional.of(WORKER_B));
        persist();

        service.registerClosedJournal(JOURNAL);

        assertThat(saved()).extracting(ShiftSaleHistory::workerId).containsExactlyInAnyOrder(WORKER_A, WORKER_B);
    }

    @Test
    void theSameWorkerReportedOnceByUuidAndOnceByCedulaForTheSameShiftIsADuplicate() {
        when(sales.findVendorClosesByDate(DATE)).thenReturn(List.of(
                close(WORKER_A, "DIA"), close("001001", "DIA")));
        givenStaff(member(WORKER_A, "DIA", "TITULAR"));
        when(workers.findWorkerIdByDocument("001001")).thenReturn(Optional.of(WORKER_A));

        assertThat(rejected().code()).isEqualTo(Code.DUPLICATE_CLOSE);
    }

    // ---- a document nobody has ----

    @Test
    void aDocumentOfNoRegisteredWorkerIsRefusedWithTheDocumentInTheMessage() {
        givenCloses("001001");
        givenStaff(member(WORKER_A, "DIA", "TITULAR"));
        when(workers.findWorkerIdByDocument("001001")).thenReturn(Optional.empty());

        ShiftSaleHistoryIntegrationException failure = rejected();

        assertThat(failure.code()).isEqualTo(Code.INVALID_VENDOR_IDENTIFIER);
        assertThat(failure.getMessage()).contains("no worker matches '001001'");
        verify(history).markPending(eq(JOURNAL), contains(Code.INVALID_VENDOR_IDENTIFIER.name()));
    }

    @Test
    void oneUnknownDocumentAmongValidSellersPreventsAnyPartialSave() {
        when(sales.findVendorClosesByDate(DATE)).thenReturn(List.of(
                close(WORKER_A, "DIA"), close("999999", "NOCHE")));
        givenStaff(member(WORKER_A, "DIA", "TITULAR"), member(WORKER_B, "NOCHE", "TITULAR"));
        when(workers.findWorkerIdByDocument("999999")).thenReturn(Optional.empty());

        assertThat(rejected().code()).isEqualTo(Code.INVALID_VENDOR_IDENTIFIER);
    }

    @ParameterizedTest
    @ValueSource(strings = {"not-a-uuid", "1-1-1-1-1"})
    void anIdentifierThatLooksLikeAnAbbreviatedUuidIsLookedUpAsADocumentAndRefusedIfNobodyHasIt(String identifier) {
        givenCloses(identifier);
        givenStaff(member(WORKER_A, "DIA", "TITULAR"));
        when(workers.findWorkerIdByDocument(identifier)).thenReturn(Optional.empty());

        assertThat(rejected().code()).isEqualTo(Code.INVALID_VENDOR_IDENTIFIER);
        verify(workers).findWorkerIdByDocument(identifier);
    }

    // ---- what is never looked up ----

    @Test
    void aFullUuidIsUsedDirectlyAndTheDirectoryIsNotAsked() {
        givenCloses(WORKER_A.toString());
        givenStaff(member(WORKER_A, "DIA", "TITULAR"));
        persist();

        service.registerClosedJournal(JOURNAL);

        assertThat(saved().getFirst().workerId()).isEqualTo(WORKER_A);
        verifyNoInteractions(workers);
    }

    @Test
    void aFullUuidInUpperCaseIsAlsoUsedDirectly() {
        givenCloses(WORKER_A.toString().toUpperCase());
        givenStaff(member(WORKER_A, "DIA", "TITULAR"));
        persist();

        service.registerClosedJournal(JOURNAL);

        assertThat(saved().getFirst().workerId()).isEqualTo(WORKER_A);
        verifyNoInteractions(workers);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void aMissingOrBlankIdentifierIsRefusedWithoutAskingTheDirectory(String identifier) {
        givenCloses(identifier);
        givenStaff(member(WORKER_A, "DIA", "TITULAR"));

        assertThat(rejected().code()).isEqualTo(Code.INVALID_VENDOR_IDENTIFIER);
        verifyNoInteractions(workers);
    }

    @Test
    void withoutAWorkerDirectoryOnlyFullUuidsIdentifyAVendor() {
        ShiftSaleHistoryRegistrationService withoutDirectory =
                new ShiftSaleHistoryRegistrationService(sales, staff, history);
        givenCloses("001001");
        givenStaff(member(WORKER_A, "DIA", "TITULAR"));
        when(history.markPending(eq(JOURNAL), anyString())).thenReturn(pending());

        assertThatThrownBy(() -> withoutDirectory.registerClosedJournal(JOURNAL))
                .isInstanceOfSatisfying(ShiftSaleHistoryIntegrationException.class,
                        failure -> assertThat(failure.code()).isEqualTo(Code.INVALID_VENDOR_IDENTIFIER));
        verify(history, never()).saveJournal(any(), anyList());
    }
}
