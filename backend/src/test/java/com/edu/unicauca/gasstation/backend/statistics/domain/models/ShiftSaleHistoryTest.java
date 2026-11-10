package com.edu.unicauca.gasstation.backend.statistics.domain.models;

import com.edu.unicauca.gasstation.backend.statistics.domain.models.ShiftSaleHistory.EffectiveRole;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ShiftSaleHistoryTest {

    private static final LocalDate DATE = LocalDate.of(2026, 11, 10);
    private static final UUID WORKER = UUID.fromString("00000000-0000-0000-0000-00000000000a");
    private static final ClosedJournalSnapshot JOURNAL =
            new ClosedJournalSnapshot(15L, DATE, 105L, DATE.plusDays(1).atTime(6, 15));
    private static final ShiftSaleIdentity IDENTITY = new ShiftSaleIdentity(DATE, "001001", "12-7");
    private static final ShiftSaleAmounts SALES = amounts("1.12300", "100.2500");

    @ParameterizedTest
    @MethodSource("effectiveRoles")
    void classifiesSupportOnlyFromTheStoredEffectiveRole(EffectiveRole role, boolean expectedSupport) {
        var history = history(role);

        assertThat(history.effectiveRole()).isEqualTo(role);
        assertThat(history.support()).isEqualTo(expectedSupport);
    }

    static Stream<Arguments> effectiveRoles() {
        return Stream.of(Arguments.of(EffectiveRole.TITULAR, false), Arguments.of(EffectiveRole.APOYO, true));
    }

    @Test
    void keepsOriginalVendorIdentifierSeparateFromTheResolvedWorkerId() {
        var history = history(EffectiveRole.APOYO);

        assertThat(history.journal()).isEqualTo(JOURNAL);
        assertThat(history.identity()).isEqualTo(IDENTITY);
        assertThat(history.workerId()).isEqualTo(WORKER);
        assertThat(history.identity().vendorDocument()).isEqualTo("001001");
        assertThat(history.identity().vendorDocument()).isNotEqualTo(history.workerId().toString());
        assertThat(history.vendorName()).isEqualTo("Ana Original");
        assertThat(history.workedCode()).isEqualTo("12-7");
    }

    @Test
    void identityDoesNotChangeWithNamesRolesOrReportedAmounts() {
        var first = history(EffectiveRole.APOYO);
        var anotherSnapshot = new ShiftSaleHistory(JOURNAL, IDENTITY, WORKER, "Ana Actualizada", "12-7",
                EffectiveRole.TITULAR, amounts("2.000", "200.00"));

        assertThat(anotherSnapshot.identity()).isEqualTo(first.identity());
        assertThat(anotherSnapshot).isNotEqualTo(first);
    }

    @Test
    void laterWorkerAndScheduleChangesDoNotRewriteTheHistoricalSnapshot() {
        var currentWorker = new CurrentWorker();
        var currentShift = new CurrentShift();
        var history = new ShiftSaleHistory(JOURNAL, IDENTITY, currentWorker.id, currentWorker.name,
                currentShift.code, currentShift.role, SALES);

        currentWorker.id = UUID.randomUUID();
        currentWorker.name = "Nombre posterior";
        currentWorker.active = false;
        currentShift.code = "NOCHE";
        currentShift.role = EffectiveRole.TITULAR;

        assertThat(history.workerId()).isEqualTo(WORKER);
        assertThat(history.vendorName()).isEqualTo("Ana Original");
        assertThat(history.workedCode()).isEqualTo("12-7");
        assertThat(history.effectiveRole()).isEqualTo(EffectiveRole.APOYO);
        assertThat(history.support()).isTrue();
        assertThat(history.sales().motorGallons()).isEqualTo(new BigDecimal("1.12300"));
        assertThat(history.sales().basketDominusAmountCop()).isEqualTo(new BigDecimal("100.2500"));
        assertThat(history.journal().closedAt()).isEqualTo(DATE.plusDays(1).atTime(6, 15));
    }

    @Test
    void rejectsIdentityOfAnotherOperationalDay() {
        var anotherDay = new ShiftSaleIdentity(DATE.plusDays(1), "001001", "12-7");

        assertThatThrownBy(() -> new ShiftSaleHistory(JOURNAL, anotherDay, WORKER, "Ana", "12-7",
                EffectiveRole.APOYO, SALES)).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("identity date must match the closed journal date");
    }

    @Test
    void rejectsTheZeroWorkerUuid() {
        assertThatThrownBy(() -> new ShiftSaleHistory(JOURNAL, IDENTITY, new UUID(0, 0), "Ana", "12-7",
                EffectiveRole.APOYO, SALES)).isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @MethodSource("missingComponents")
    void rejectsMissingHistoricalData(ClosedJournalSnapshot journal, ShiftSaleIdentity identity,
            UUID worker, String name, String code, EffectiveRole role, ShiftSaleAmounts sales, String field) {
        assertThatThrownBy(() -> new ShiftSaleHistory(journal, identity, worker, name, code, role, sales))
                .isInstanceOf(NullPointerException.class).hasMessage(field);
    }

    static Stream<Arguments> missingComponents() {
        return Stream.of(
                Arguments.of(null, IDENTITY, WORKER, "Ana", "12-7", EffectiveRole.APOYO, SALES, "journal"),
                Arguments.of(JOURNAL, null, WORKER, "Ana", "12-7", EffectiveRole.APOYO, SALES, "identity"),
                Arguments.of(JOURNAL, IDENTITY, null, "Ana", "12-7", EffectiveRole.APOYO, SALES, "workerId"),
                Arguments.of(JOURNAL, IDENTITY, WORKER, null, "12-7", EffectiveRole.APOYO, SALES, "vendorName"),
                Arguments.of(JOURNAL, IDENTITY, WORKER, "Ana", null, EffectiveRole.APOYO, SALES, "workedCode"),
                Arguments.of(JOURNAL, IDENTITY, WORKER, "Ana", "12-7", null, SALES, "effectiveRole"),
                Arguments.of(JOURNAL, IDENTITY, WORKER, "Ana", "12-7", EffectiveRole.APOYO, null, "sales"));
    }

    @ParameterizedTest
    @MethodSource("blankDescriptions")
    void rejectsBlankSellerNameOrWorkedCode(String name, String code) {
        assertThatThrownBy(() -> new ShiftSaleHistory(JOURNAL, IDENTITY, WORKER, name, code,
                EffectiveRole.APOYO, SALES)).isInstanceOf(IllegalArgumentException.class);
    }

    static Stream<Arguments> blankDescriptions() {
        return Stream.of(Arguments.of("", "12-7"), Arguments.of(" \t", "12-7"),
                Arguments.of("Ana", ""), Arguments.of("Ana", " \t"));
    }

    private static ShiftSaleHistory history(EffectiveRole role) {
        return new ShiftSaleHistory(JOURNAL, IDENTITY, WORKER, "Ana Original", "12-7", role, SALES);
    }

    private static ShiftSaleAmounts amounts(String gallons, String cop) {
        var volume = new BigDecimal(gallons);
        var money = new BigDecimal(cop);
        return new ShiftSaleAmounts(volume, volume, volume, volume, money, money, money);
    }

    private static class CurrentWorker {
        UUID id = WORKER;
        String name = "Ana Original";
        boolean active = true;
    }

    private static class CurrentShift {
        String code = "12-7";
        EffectiveRole role = EffectiveRole.APOYO;
    }
}
