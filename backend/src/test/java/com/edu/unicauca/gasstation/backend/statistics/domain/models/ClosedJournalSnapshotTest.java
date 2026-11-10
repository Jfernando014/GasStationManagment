package com.edu.unicauca.gasstation.backend.statistics.domain.models;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ClosedJournalSnapshotTest {

    private static final LocalDate DATE = LocalDate.of(2026, 11, 10);
    private static final LocalDateTime CLOSED_AT = DATE.plusDays(1).atTime(6, 15, 30);

    @Test
    void preservesJournalMetadataAndOperationalDateAcrossMidnight() {
        var journal = new ClosedJournalSnapshot(15L, DATE, 105L, CLOSED_AT);

        assertThat(journal.journalId()).isEqualTo(15L);
        assertThat(journal.consecutive()).isEqualTo(105L);
        assertThat(journal.journalDate()).isEqualTo(DATE);
        assertThat(journal.closedAt()).isEqualTo(CLOSED_AT);
        assertThat(journal.journalDate()).isNotEqualTo(journal.closedAt().toLocalDate());
    }

    @Test
    void acceptsThePublicEventVariantWithoutDatabaseId() {
        var journal = new ClosedJournalSnapshot(null, DATE, 105L, CLOSED_AT);

        assertThat(journal.journalId()).isNull();
        assertThat(journal.journalDate()).isEqualTo(DATE);
    }

    @ParameterizedTest
    @MethodSource("missingMetadata")
    void rejectsMissingClosingMetadata(LocalDate date, Long consecutive, LocalDateTime closedAt, String field) {
        assertThatThrownBy(() -> new ClosedJournalSnapshot(15L, date, consecutive, closedAt))
                .isInstanceOf(NullPointerException.class).hasMessage(field);
    }

    static Stream<Arguments> missingMetadata() {
        return Stream.of(
                Arguments.of(null, 105L, CLOSED_AT, "journalDate"),
                Arguments.of(DATE, null, CLOSED_AT, "consecutive"),
                Arguments.of(DATE, 105L, null, "closedAt"));
    }

    @ParameterizedTest
    @MethodSource("invalidIdentifiers")
    void rejectsNonpositiveJournalIdentifiers(Long journalId, Long consecutive) {
        assertThatThrownBy(() -> new ClosedJournalSnapshot(journalId, DATE, consecutive, CLOSED_AT))
                .isInstanceOf(IllegalArgumentException.class);
    }

    static Stream<Arguments> invalidIdentifiers() {
        return Stream.of(Arguments.of(0L, 105L), Arguments.of(-1L, 105L),
                Arguments.of(15L, 0L), Arguments.of(15L, -1L));
    }
}
