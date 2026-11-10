package com.edu.unicauca.gasstation.backend.statistics.domain.models;

import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JournalSalesProcessingTest {
    private static final ClosedJournalSnapshot JOURNAL = new ClosedJournalSnapshot(
            null, LocalDate.of(2026, 11, 10), 105L, LocalDate.of(2026, 11, 11).atTime(6, 0));
    private static final Instant NOW = Instant.parse("2026-11-11T11:00:00Z");

    @Test
    void anEmptyProcessedDayIsDifferentFromAPendingDay() {
        var empty = new JournalSalesProcessing(JOURNAL, JournalSalesProcessing.Status.PROCESSED, 1, 0, null, NOW, NOW);
        var pending = new JournalSalesProcessing(JOURNAL, JournalSalesProcessing.Status.PENDING, 1, null, "Unresolved seller", null, NOW);

        assertThat(empty.saleCount()).isZero();
        assertThat(pending.saleCount()).isNull();
        assertThat(pending.processedAt()).isNull();
    }

    @Test
    void rejectsProcessedStateWithoutACompleteSuccessfulResult() {
        assertThatThrownBy(() -> new JournalSalesProcessing(JOURNAL, JournalSalesProcessing.Status.PROCESSED,
                0, 0, null, NOW, NOW)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new JournalSalesProcessing(JOURNAL, JournalSalesProcessing.Status.PROCESSED,
                1, null, null, NOW, NOW)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new JournalSalesProcessing(JOURNAL, JournalSalesProcessing.Status.PROCESSED,
                1, -1, null, NOW, NOW)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new JournalSalesProcessing(JOURNAL, JournalSalesProcessing.Status.PROCESSED,
                1, 1, "failure", NOW, NOW)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new JournalSalesProcessing(JOURNAL, JournalSalesProcessing.Status.PROCESSED,
                1, 1, null, null, NOW)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsPendingStateThatClaimsSuccess() {
        assertThatThrownBy(() -> new JournalSalesProcessing(JOURNAL, JournalSalesProcessing.Status.PENDING,
                1, 0, null, null, NOW)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new JournalSalesProcessing(JOURNAL, JournalSalesProcessing.Status.PENDING,
                1, null, null, NOW, NOW)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsMissingMetadataAndNegativeAttempts() {
        assertThatThrownBy(() -> new JournalSalesProcessing(null, JournalSalesProcessing.Status.PENDING,
                0, null, null, null, NOW)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new JournalSalesProcessing(JOURNAL, null,
                0, null, null, null, NOW)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new JournalSalesProcessing(JOURNAL, JournalSalesProcessing.Status.PENDING,
                0, null, null, null, null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new JournalSalesProcessing(JOURNAL, JournalSalesProcessing.Status.PENDING,
                -1, null, null, null, NOW)).isInstanceOf(IllegalArgumentException.class);
    }
}
