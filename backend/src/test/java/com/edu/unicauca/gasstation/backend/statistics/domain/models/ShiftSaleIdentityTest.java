package com.edu.unicauca.gasstation.backend.statistics.domain.models;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ShiftSaleIdentityTest {

    private static final LocalDate DATE = LocalDate.of(2026, 11, 10);

    @Test
    void repeatedSourceCloseHasTheSameBusinessKey() {
        var first = new ShiftSaleIdentity(DATE, "001001", "NOCHE");
        var repeated = new ShiftSaleIdentity(DATE, "001001", "NOCHE");
        var keys = new HashSet<ShiftSaleIdentity>();

        assertThat(keys.add(first)).isTrue();
        assertThat(keys.add(repeated)).isFalse();
        assertThat(repeated).isEqualTo(first);
        assertThat(repeated.hashCode()).isEqualTo(first.hashCode());
    }

    @Test
    void distinguishesDatesVendorsAndShiftsWithoutCollapsingSeveralCloses() {
        var key = new ShiftSaleIdentity(DATE, "001001", "DIA");

        assertThat(new HashSet<>(java.util.List.of(key,
                new ShiftSaleIdentity(DATE.plusDays(1), "001001", "DIA"),
                new ShiftSaleIdentity(DATE, "001002", "DIA"),
                new ShiftSaleIdentity(DATE, "001001", "NOCHE")))).hasSize(4);
    }

    @Test
    void keepsOriginalIdentifiersWithoutParsingOrNormalizingThem() {
        var document = new ShiftSaleIdentity(DATE, "001001", " NOCHE ");
        String uuid = "00000000-0000-0000-0000-00000000000a";
        var workerIdentifier = new ShiftSaleIdentity(DATE, uuid, "NOCHE");

        assertThat(document.vendorDocument()).isEqualTo("001001");
        assertThat(document.shiftName()).isEqualTo(" NOCHE ");
        assertThat(workerIdentifier.vendorDocument()).isEqualTo(uuid).hasSize(36);
    }

    @ParameterizedTest
    @MethodSource("missingComponents")
    void rejectsMissingKeyComponents(LocalDate date, String document, String shift, String field) {
        assertThatThrownBy(() -> new ShiftSaleIdentity(date, document, shift))
                .isInstanceOf(NullPointerException.class).hasMessage(field);
    }

    static Stream<Arguments> missingComponents() {
        return Stream.of(Arguments.of(null, "001001", "DIA", "journalDate"),
                Arguments.of(DATE, null, "DIA", "vendorDocument"),
                Arguments.of(DATE, "001001", null, "shiftName"));
    }

    @ParameterizedTest
    @MethodSource("blankComponents")
    void rejectsBlankVendorOrShift(String document, String shift) {
        assertThatThrownBy(() -> new ShiftSaleIdentity(DATE, document, shift))
                .isInstanceOf(IllegalArgumentException.class);
    }

    static Stream<Arguments> blankComponents() {
        return Stream.of(Arguments.of("", "DIA"), Arguments.of(" \t", "DIA"),
                Arguments.of("001001", ""), Arguments.of("001001", " \t"));
    }
}
