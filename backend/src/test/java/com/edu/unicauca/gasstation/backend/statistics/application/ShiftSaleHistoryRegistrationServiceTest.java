package com.edu.unicauca.gasstation.backend.statistics.application;

import com.edu.unicauca.gasstation.backend.sales.SellerCloseQuery;
import com.edu.unicauca.gasstation.backend.shifts.daystaff.DayStaff;
import com.edu.unicauca.gasstation.backend.shifts.daystaff.DayStaffQuery;
import com.edu.unicauca.gasstation.backend.statistics.domain.models.ShiftSaleAmounts;
import com.edu.unicauca.gasstation.backend.statistics.domain.models.ShiftSaleHistory;
import com.edu.unicauca.gasstation.backend.statistics.domain.repositories.ShiftSaleHistoryRepository;
import com.edu.unicauca.gasstation.backend.statistics.exception.ShiftSaleHistoryIntegrationException;
import com.edu.unicauca.gasstation.backend.statistics.exception.ShiftSaleHistoryIntegrationException.Code;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static com.edu.unicauca.gasstation.backend.statistics.ShiftSaleRegistrationTestData.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ShiftSaleHistoryRegistrationServiceTest {
    @Mock SellerCloseQuery sales;
    @Mock DayStaffQuery staff;
    @Mock ShiftSaleHistoryRepository history;
    @InjectMocks ShiftSaleHistoryRegistrationService service;

    @Test
    void registersAKnownTitularWithoutUsingNamesOrPumpForIdentityOrRole() {
        var source = close(DATE, WORKER_A.toString(), "NOCHE", "Recorded seller", 3, AMOUNTS);
        when(sales.findVendorClosesByDate(DATE)).thenReturn(List.of(source));
        when(staff.getDayStaff(DATE)).thenReturn(day(member(WORKER_A, "NOCHE", "TITULAR")));
        persist();

        var result = service.registerClosedJournal(JOURNAL);

        assertThat(result.saleCount()).isEqualTo(1);
        var snapshot = saved().getFirst();
        assertThat(snapshot.workerId()).isEqualTo(WORKER_A);
        assertThat(snapshot.vendorName()).isEqualTo("Recorded seller");
        assertThat(snapshot.workedCode()).isEqualTo("NOCHE");
        assertThat(snapshot.effectiveRole()).isEqualTo(ShiftSaleHistory.EffectiveRole.TITULAR);
        assertThat(snapshot.support()).isFalse();
        assertThat(snapshot.journal()).isEqualTo(JOURNAL);
        assertThat(snapshot.identity().journalDate()).isEqualTo(DATE);
        verify(sales).findVendorClosesByDate(DATE);
        verify(staff).getDayStaff(DATE);
        verifyNoMoreInteractions(sales, staff);
    }

    @Test
    void registersSupportFromMemberEvenWhenTheCloseUsesTheTitularPump() {
        when(sales.findVendorClosesByDate(DATE)).thenReturn(List.of(close(WORKER_A, "12-7")));
        when(staff.getDayStaff(DATE)).thenReturn(day(member(WORKER_A, "12-7", "APOYO")));
        persist();

        service.registerClosedJournal(JOURNAL);

        assertThat(saved().getFirst().support()).isTrue();
        assertThat(saved().getFirst().effectiveRole()).isEqualTo(ShiftSaleHistory.EffectiveRole.APOYO);
    }

    @Test
    void confirmedShiftChangeUsesTheWorkedRoleInsteadOfTheOriginalGridRole() {
        var novelty = new DayStaff.Novelty(DayStaff.NoveltyType.SHIFT_CHANGE, null, null, "Exceptional coverage");
        var effective = member(WORKER_A, "12-7", "DIA", "APOYO", novelty);
        when(sales.findVendorClosesByDate(DATE)).thenReturn(List.of(close(WORKER_A, "12-7")));
        when(staff.getDayStaff(DATE)).thenReturn(day(effective));
        persist();

        service.registerClosedJournal(JOURNAL);

        assertThat(saved().getFirst().support()).isTrue();
        assertThat(saved().getFirst().workedCode()).isEqualTo("12-7");
    }

    @Test
    void replacementUsesTheReplacementWorkerAndTheRoleOfTheCoveredShift() {
        var novelty = new DayStaff.Novelty(DayStaff.NoveltyType.REPLACEMENT, WORKER_A, "Absent seller", "Replacement");
        var effective = member(WORKER_B, "DIA", "DESCANSO", "TITULAR", novelty);
        var absentee = new DayStaff.Absentee(WORKER_A, "Recorded seller", "DIA",
                DayStaff.NoveltyType.REPLACEMENT, "Replacement");
        when(sales.findVendorClosesByDate(DATE)).thenReturn(List.of(close(WORKER_B, "DIA")));
        when(staff.getDayStaff(DATE)).thenReturn(new DayStaff(DATE, List.of(effective), List.of(absentee),
                DayStaff.Step1Status.CONFIRMED, true, List.of()));
        persist();

        service.registerClosedJournal(JOURNAL);

        assertThat(saved().getFirst().workerId()).isEqualTo(WORKER_B);
        assertThat(saved().getFirst().support()).isFalse();
    }

    @Test
    void severalSellersUseOneStaffQueryAndOneAtomicSave() {
        when(sales.findVendorClosesByDate(DATE)).thenReturn(List.of(close(WORKER_A, "DIA"), close(WORKER_B, "12-7")));
        when(staff.getDayStaff(DATE)).thenReturn(day(member(WORKER_A, "DIA", "TITULAR"),
                member(WORKER_B, "12-7", "APOYO")));
        persist();

        assertThat(service.registerClosedJournal(JOURNAL).saleCount()).isEqualTo(2);

        assertThat(saved()).extracting(ShiftSaleHistory::workerId).containsExactly(WORKER_A, WORKER_B);
        verify(staff, times(1)).getDayStaff(DATE);
        verify(history, times(1)).saveJournal(eq(JOURNAL), anyList());
    }

    @Test
    void severalClosesOfOneSellerRequireAnExplicitEffectiveMemberForEachShift() {
        when(sales.findVendorClosesByDate(DATE)).thenReturn(List.of(close(WORKER_A, "DIA"), close(WORKER_A, "12-7")));
        when(staff.getDayStaff(DATE)).thenReturn(day(member(WORKER_A, "DIA", "TITULAR"),
                member(WORKER_A, "12-7", "APOYO")));
        persist();

        service.registerClosedJournal(JOURNAL);

        assertThat(saved()).extracting(ShiftSaleHistory::workedCode).containsExactly("DIA", "12-7");
        assertThat(saved()).extracting(ShiftSaleHistory::support).containsExactly(false, true);
    }

    @Test
    void oneEffectiveShiftCannotCertifyAnotherCloseOfTheSameSeller() {
        when(sales.findVendorClosesByDate(DATE)).thenReturn(List.of(close(WORKER_A, "DIA"), close(WORKER_A, "NOCHE")));
        when(staff.getDayStaff(DATE)).thenReturn(day(member(WORKER_A, "DIA", "TITULAR")));

        incident(Code.SHIFT_MISMATCH);
    }

    @Test
    void keepsUppercaseUuidTextWhileResolvingItsKnownWorker() {
        String original = WORKER_A.toString().toUpperCase(Locale.ROOT);
        when(sales.findVendorClosesByDate(DATE)).thenReturn(List.of(close(original, "DIA")));
        when(staff.getDayStaff(DATE)).thenReturn(day(member(WORKER_A, "DIA", "TITULAR")));
        persist();

        service.registerClosedJournal(JOURNAL);

        assertThat(saved().getFirst().identity().vendorDocument()).isEqualTo(original);
        assertThat(saved().getFirst().workerId()).isEqualTo(WORKER_A);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"001001", "not-a-uuid", "1-1-1-1-1", " aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa "})
    void rejectsCedulasAndMalformedIdentifiersWithoutNameFallback(String document) {
        when(sales.findVendorClosesByDate(DATE)).thenReturn(List.of(close(document, "DIA")));
        when(staff.getDayStaff(DATE)).thenReturn(day(member(WORKER_A, "DIA", "TITULAR")));

        incident(Code.INVALID_VENDOR_IDENTIFIER);
    }

    @Test
    void unknownUuidDoesNotMatchAnotherWorkerByName() {
        when(sales.findVendorClosesByDate(DATE)).thenReturn(List.of(close(WORKER_B, "DIA")));
        when(staff.getDayStaff(DATE)).thenReturn(day(member(WORKER_A, "DIA", "TITULAR")));

        incident(Code.WORKER_NOT_IN_DAY_STAFF);
    }

    @Test
    void aWorkerOmittedByTheSourceIsPendingIncludingInactiveWorkers() {
        when(sales.findVendorClosesByDate(DATE)).thenReturn(List.of(close(WORKER_A, "DIA")));
        when(staff.getDayStaff(DATE)).thenReturn(day());

        incident(Code.WORKER_NOT_IN_DAY_STAFF);
    }

    @Test
    void anAbsenteeCannotBeUsedAsEffectiveStaff() {
        var absentee = new DayStaff.Absentee(WORKER_A, "Recorded seller", "DIA",
                DayStaff.NoveltyType.ABSENCE, "Absent");
        when(sales.findVendorClosesByDate(DATE)).thenReturn(List.of(close(WORKER_A, "DIA")));
        when(staff.getDayStaff(DATE)).thenReturn(new DayStaff(DATE, List.of(), List.of(absentee),
                DayStaff.Step1Status.CONFIRMED, true, List.of()));

        incident(Code.WORKER_NOT_IN_DAY_STAFF);
    }

    @ParameterizedTest
    @ValueSource(strings = {"NOCHE", "Dia", " DIA "})
    void requiresExactWorkedCodeWithoutAliasesOrWhitespaceGuessing(String shift) {
        when(sales.findVendorClosesByDate(DATE)).thenReturn(List.of(close(WORKER_A, shift)));
        when(staff.getDayStaff(DATE)).thenReturn(day(member(WORKER_A, "DIA", "TITULAR")));

        incident(Code.SHIFT_MISMATCH);
    }

    @Test
    void sourceCloseOfAnotherDateCannotBeRelabelledAsTheClosedDay() {
        when(sales.findVendorClosesByDate(DATE)).thenReturn(List.of(
                close(DATE.plusDays(1), WORKER_A.toString(), "DIA", "Seller", 1, AMOUNTS)));

        incident(Code.INVALID_SALES_RESPONSE);
        verifyNoInteractions(staff);
    }

    @Test
    void availableEmptySalesAreProcessedWithoutQueryingUnneededStaffOrInventingSales() {
        when(sales.findVendorClosesByDate(DATE)).thenReturn(List.of());
        persist();

        assertThat(service.registerClosedJournal(JOURNAL).saleCount()).isZero();

        assertThat(saved()).isEmpty();
        verifyNoInteractions(staff);
        verify(history, never()).markPending(any(), anyString());
    }

    @Test
    void salesQueryFailureIsPendingAndIsNeverConvertedToEmptySales() {
        var failure = new IllegalStateException("sales unavailable");
        when(sales.findVendorClosesByDate(DATE)).thenThrow(failure);

        queryFailure(failure);
        verifyNoInteractions(staff);
    }

    @Test
    void staffQueryFailureIsPendingAndNeverConvertedToAnEmptyDay() {
        var failure = new IllegalStateException("staff unavailable");
        when(sales.findVendorClosesByDate(DATE)).thenReturn(List.of(close(WORKER_A, "DIA")));
        when(staff.getDayStaff(DATE)).thenThrow(failure);

        queryFailure(failure);
    }

    @Test
    void nullSalesResponseIsAnIncidentNotAnEmptySuccessfulDay() {
        when(sales.findVendorClosesByDate(DATE)).thenReturn(null);

        incident(Code.INVALID_SALES_RESPONSE);
        verifyNoInteractions(staff);
    }

    @Test
    void nullCloseInsideAnOtherwiseAvailableResponseIsNotDiscarded() {
        when(sales.findVendorClosesByDate(DATE)).thenReturn(Arrays.asList(close(WORKER_A, "DIA"), null));

        incident(Code.INVALID_SALES_RESPONSE);
        verifyNoInteractions(staff);
    }

    @Test
    void nullStaffResponseIsAnIncident() {
        when(sales.findVendorClosesByDate(DATE)).thenReturn(List.of(close(WORKER_A, "DIA")));
        when(staff.getDayStaff(DATE)).thenReturn(null);

        incident(Code.INVALID_STAFF_RESPONSE);
    }

    @Test
    void staffOfAnotherOperationalDateIsAnIncident() {
        when(sales.findVendorClosesByDate(DATE)).thenReturn(List.of(close(WORKER_A, "DIA")));
        when(staff.getDayStaff(DATE)).thenReturn(new DayStaff(DATE.plusDays(1),
                List.of(member(WORKER_A, "DIA", "TITULAR")), List.of(), DayStaff.Step1Status.CONFIRMED, true, List.of()));

        incident(Code.INVALID_STAFF_RESPONSE);
    }

    @ParameterizedTest
    @EnumSource(value = DayStaff.Step1Status.class, names = "CONFIRMED", mode = EnumSource.Mode.EXCLUDE)
    void unconfirmedStaffCannotCertifyEffectiveRolesOrNovelties(DayStaff.Step1Status status) {
        when(sales.findVendorClosesByDate(DATE)).thenReturn(List.of(close(WORKER_A, "DIA")));
        when(staff.getDayStaff(DATE)).thenReturn(new DayStaff(DATE, List.of(member(WORKER_A, "DIA", "TITULAR")),
                List.of(), status, false, List.of()));

        incident(Code.STAFF_NOT_CONFIRMED);
    }

    @Test
    void confirmedStatusWithoutAppliedNoveltiesIsInsufficient() {
        when(sales.findVendorClosesByDate(DATE)).thenReturn(List.of(close(WORKER_A, "DIA")));
        when(staff.getDayStaff(DATE)).thenReturn(new DayStaff(DATE, List.of(member(WORKER_A, "DIA", "TITULAR")),
                List.of(), DayStaff.Step1Status.CONFIRMED, false, List.of()));

        incident(Code.STAFF_NOT_CONFIRMED);
    }

    @Test
    void unsupportedEffectiveRolesAreNotApproximatedToTitular() {
        when(sales.findVendorClosesByDate(DATE)).thenReturn(List.of(close(WORKER_A, "DIA")));
        when(staff.getDayStaff(DATE)).thenReturn(day(member(WORKER_A, "DIA", "OTHER")));

        incident(Code.UNSUPPORTED_ROLE);
    }

    @Test
    void ambiguousEffectiveMembersAreNotResolvedByTakingTheFirst() {
        var member = member(WORKER_A, "DIA", "TITULAR");
        when(sales.findVendorClosesByDate(DATE)).thenReturn(List.of(close(WORKER_A, "DIA")));
        when(staff.getDayStaff(DATE)).thenReturn(day(member, member));

        incident(Code.AMBIGUOUS_SHIFT);
    }

    @Test
    void duplicateSourceClosesPreventConfirmationOfTheWholeDay() {
        var close = close(WORKER_A, "DIA");
        when(sales.findVendorClosesByDate(DATE)).thenReturn(List.of(close, close));
        when(staff.getDayStaff(DATE)).thenReturn(day(member(WORKER_A, "DIA", "TITULAR")));

        incident(Code.DUPLICATE_CLOSE);
    }

    @Test
    void differentTextRepresentationsOfOneWorkerCannotDuplicateTheSameWorkedShift() {
        when(sales.findVendorClosesByDate(DATE)).thenReturn(List.of(close(WORKER_A, "DIA"),
                close(WORKER_A.toString().toUpperCase(Locale.ROOT), "DIA")));
        when(staff.getDayStaff(DATE)).thenReturn(day(member(WORKER_A, "DIA", "TITULAR")));

        incident(Code.DUPLICATE_CLOSE);
    }

    @Test
    void monetaryAndGallonObservationsKeepTheirMeaningsPrecisionAndScales() {
        var close = close(WORKER_A, "DIA");
        when(sales.findVendorClosesByDate(DATE)).thenReturn(List.of(close));
        when(staff.getDayStaff(DATE)).thenReturn(day(member(WORKER_A, "DIA", "TITULAR")));
        persist();

        service.registerClosedJournal(JOURNAL);

        var amounts = saved().getFirst().sales();
        List<BigDecimal> actual = List.of(amounts.motorGallons(), amounts.dieselGallons(), amounts.extraGallons(),
                amounts.maxproGallons(), amounts.basketDominusAmountCop(), amounts.dominusTirillaCop(),
                amounts.totalDeliveredCop());
        List<BigDecimal> source = List.of(close.gallonsMotor(), close.gallonsDiesel(), close.gallonsExtra(),
                close.gallonsMaxpro(), close.basketDominusAmount(), close.dominusTirilla(), close.totalDelivered());
        for (int index = 0; index < source.size(); index++) {
            assertThat(actual.get(index)).isEqualTo(source.get(index));
            assertThat(actual.get(index).scale()).isEqualTo(source.get(index).scale());
        }
        assertThat(amounts.dominusTirillaCop()).isNotEqualByComparingTo(amounts.totalDeliveredCop());
    }

    @Test
    void missingRecordedAmountIsNotReplacedWithZero() {
        var missing = new SellerCloseQuery.VendorCloseSummary(DATE, WORKER_A.toString(), "Recorded seller", "DIA", 1,
                null, null, null, null, null, null, null, null, null, null, null, null, null, "CUADRA", List.of());
        when(sales.findVendorClosesByDate(DATE)).thenReturn(List.of(missing));
        when(staff.getDayStaff(DATE)).thenReturn(day(member(WORKER_A, "DIA", "TITULAR")));

        incident(Code.INVALID_SALE);
    }

    @Test
    void explicitZeroAmountsAreValidRecordedObservations() {
        var zero = new BigDecimal("0.0000");
        var amounts = new ShiftSaleAmounts(zero, zero, zero, zero, zero, zero, zero);
        when(sales.findVendorClosesByDate(DATE)).thenReturn(List.of(
                close(DATE, WORKER_A.toString(), "DIA", "Seller", 1, amounts)));
        when(staff.getDayStaff(DATE)).thenReturn(day(member(WORKER_A, "DIA", "TITULAR")));
        persist();

        service.registerClosedJournal(JOURNAL);

        assertThat(saved().getFirst().sales()).isEqualTo(amounts);
    }

    @Test
    void oneUnresolvedSellerAfterAValidSellerPreventsAnyPartialSave() {
        when(sales.findVendorClosesByDate(DATE)).thenReturn(List.of(close(WORKER_A, "DIA"), close("001001", "12-7")));
        when(staff.getDayStaff(DATE)).thenReturn(day(member(WORKER_A, "DIA", "TITULAR"),
                member(WORKER_B, "12-7", "APOYO")));

        incident(Code.INVALID_VENDOR_IDENTIFIER);
    }

    @Test
    void confirmedDayReturnsStoredStateWithoutReadingMutableSources() {
        var processed = processed(2);
        when(history.findProcessing(DATE)).thenReturn(Optional.of(processed));

        assertThat(service.registerClosedJournal(JOURNAL)).isEqualTo(processed);

        verifyNoInteractions(sales, staff);
        verify(history, never()).saveJournal(any(), anyList());
        verify(history, never()).markPending(any(), anyString());
    }

    @Test
    void pendingDayIsRetriedUsingSourcesAndTheExistingRepository() {
        when(history.findProcessing(DATE)).thenReturn(Optional.of(pending()));
        when(sales.findVendorClosesByDate(DATE)).thenReturn(List.of(close(WORKER_A, "DIA")));
        when(staff.getDayStaff(DATE)).thenReturn(day(member(WORKER_A, "DIA", "TITULAR")));
        persist();

        assertThat(service.registerClosedJournal(JOURNAL).status().name()).isEqualTo("PROCESSED");
    }

    @Test
    void repositoryStorageFailureIsNotMarkedPendingASecondTimeByTheService() {
        var failure = new IllegalStateException("repository already rolled back and marked pending");
        when(sales.findVendorClosesByDate(DATE)).thenReturn(List.of());
        when(history.saveJournal(JOURNAL, List.of())).thenThrow(failure);

        assertThatThrownBy(() -> service.registerClosedJournal(JOURNAL)).isSameAs(failure);

        verify(history, never()).markPending(any(), anyString());
    }

    @Test
    void failureToRecordAnIncidentDoesNotHideTheOriginalSourceFailure() {
        var original = new IllegalStateException("sales offline");
        var markerFailure = new IllegalStateException("storage offline");
        when(sales.findVendorClosesByDate(DATE)).thenThrow(original);
        when(history.markPending(eq(JOURNAL), anyString())).thenThrow(markerFailure);

        assertThatThrownBy(() -> service.registerClosedJournal(JOURNAL)).isSameAs(original);

        assertThat(original.getSuppressed()).containsExactly(markerFailure);
        verify(history, never()).saveJournal(any(), anyList());
    }

    @Test
    void concurrentConfirmationTakesPrecedenceOverALateSourceFailure() {
        var processed = processed(1);
        when(sales.findVendorClosesByDate(DATE)).thenThrow(new IllegalStateException("source failed late"));
        when(history.markPending(eq(JOURNAL), anyString())).thenReturn(processed);

        assertThat(service.registerClosedJournal(JOURNAL)).isEqualTo(processed);

        verify(history, never()).saveJournal(any(), anyList());
    }

    @Test
    void missingJournalIsRejectedBeforeAnyQueryOrWrite() {
        assertThatThrownBy(() -> service.registerClosedJournal(null)).isInstanceOf(NullPointerException.class);

        verifyNoInteractions(history, sales, staff);
    }

    private void incident(Code code) {
        when(history.markPending(eq(JOURNAL), anyString())).thenReturn(pending());
        assertThatThrownBy(() -> service.registerClosedJournal(JOURNAL))
                .isInstanceOfSatisfying(ShiftSaleHistoryIntegrationException.class,
                        failure -> assertThat(failure.code()).isEqualTo(code));
        verify(history).markPending(eq(JOURNAL), contains(code.name()));
        verify(history, never()).saveJournal(any(), anyList());
    }

    private void queryFailure(RuntimeException failure) {
        when(history.markPending(eq(JOURNAL), anyString())).thenReturn(pending());
        assertThatThrownBy(() -> service.registerClosedJournal(JOURNAL)).isSameAs(failure);
        verify(history).markPending(eq(JOURNAL), contains(failure.getMessage()));
        verify(history, never()).saveJournal(any(), anyList());
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
}
