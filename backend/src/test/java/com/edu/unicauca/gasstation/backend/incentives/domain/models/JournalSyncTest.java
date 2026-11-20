package com.edu.unicauca.gasstation.backend.incentives.domain.models;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class JournalSyncTest {

    private static final LocalDate DATE = LocalDate.of(2026, 4, 18);
    private static final Instant FIRST = Instant.parse("2026-04-18T23:00:00Z");
    private static final Instant SECOND = Instant.parse("2026-04-19T10:00:00Z");
    private static final Instant THIRD = Instant.parse("2026-04-20T10:00:00Z");
    private static final LocalDateTime CLOSED_AT = LocalDateTime.of(2026, 4, 18, 18, 30, 5);

    private static final JournalClosure CLOSING = new JournalClosure(DATE, 42L, 118L, CLOSED_AT);

    @Test
    void theFirstSuccessfulProcessingIsProcessedAtTheFirstAttemptWithTheDataOfTheClosing() {
        JournalSync sync = JournalSync.processed(null, CLOSING, FIRST);

        assertThat(sync.journalDate()).isEqualTo(DATE);
        assertThat(sync.status()).isEqualTo(SyncStatus.PROCESSED);
        assertThat(sync.attempts()).isEqualTo(1);
        assertThat(sync.journalId()).isEqualTo(42L);
        assertThat(sync.consecutive()).isEqualTo(118L);
        assertThat(sync.closedAt()).isEqualTo(CLOSED_AT);
        assertThat(sync.lastError()).isNull();
        assertThat(sync.processedAt()).isEqualTo(FIRST);
        assertThat(sync.updatedAt()).isEqualTo(FIRST);
    }

    @Test
    void theFirstFailureIsPendingWithTheReasonAndNoSuccessfulProcessingTime() {
        JournalSync sync = JournalSync.pending(null, CLOSING, "La base no responde", FIRST);

        assertThat(sync.status()).isEqualTo(SyncStatus.PENDING);
        assertThat(sync.attempts()).isEqualTo(1);
        assertThat(sync.lastError()).isEqualTo("La base no responde");
        assertThat(sync.processedAt()).isNull();
        assertThat(sync.updatedAt()).isEqualTo(FIRST);
    }

    @Test
    void everyNewAttemptAddsOneWhateverTheResult() {
        JournalSync failed = JournalSync.pending(null, CLOSING, "falló", FIRST);
        JournalSync failedAgain = JournalSync.pending(failed, CLOSING, "falló otra vez", SECOND);
        JournalSync processed = JournalSync.processed(failedAgain, CLOSING, THIRD);
        JournalSync reprocessed = JournalSync.processed(processed, CLOSING, THIRD.plusSeconds(60));

        assertThat(failedAgain.attempts()).isEqualTo(2);
        assertThat(processed.attempts()).isEqualTo(3);
        assertThat(reprocessed.attempts()).isEqualTo(4);
    }

    @Test
    void processingAfterAFailureClearsTheError() {
        JournalSync failed = JournalSync.pending(null, CLOSING, "falló", FIRST);

        JournalSync processed = JournalSync.processed(failed, CLOSING, SECOND);

        assertThat(processed.status()).isEqualTo(SyncStatus.PROCESSED);
        assertThat(processed.lastError()).isNull();
        assertThat(processed.processedAt()).isEqualTo(SECOND);
    }

    @Test
    void aFailureAfterASuccessKeepsTheTimeOfTheLastSuccessfulProcessing() {
        JournalSync processed = JournalSync.processed(null, CLOSING, FIRST);

        JournalSync failed = JournalSync.pending(processed, JournalClosure.ofDate(DATE), "falló", SECOND);

        assertThat(failed.status()).isEqualTo(SyncStatus.PENDING);
        assertThat(failed.processedAt()).isEqualTo(FIRST);
        assertThat(failed.updatedAt()).isEqualTo(SECOND);
    }

    @Test
    void processingByHandKeepsTheDataOfTheClosingThatAlreadyArrived() {
        JournalSync processed = JournalSync.processed(null, CLOSING, FIRST);

        JournalSync byHand = JournalSync.processed(processed, JournalClosure.ofDate(DATE), SECOND);

        assertThat(byHand.journalId()).isEqualTo(42L);
        assertThat(byHand.consecutive()).isEqualTo(118L);
        assertThat(byHand.closedAt()).isEqualTo(CLOSED_AT);
    }

    @Test
    void aNewClosingReplacesTheDataKept() {
        JournalSync processed = JournalSync.processed(null, CLOSING, FIRST);
        LocalDateTime later = CLOSED_AT.plusHours(1);

        JournalSync again = JournalSync.processed(processed, new JournalClosure(DATE, 43L, 119L, later), SECOND);

        assertThat(again.journalId()).isEqualTo(43L);
        assertThat(again.consecutive()).isEqualTo(119L);
        assertThat(again.closedAt()).isEqualTo(later);
    }

    @Test
    void aDateProcessedByHandWithoutAnyClosingHasNoJournalData() {
        JournalSync sync = JournalSync.processed(null, JournalClosure.ofDate(DATE), FIRST);

        assertThat(sync.journalId()).isNull();
        assertThat(sync.consecutive()).isNull();
        assertThat(sync.closedAt()).isNull();
        assertThat(sync.attempts()).isEqualTo(1);
    }

    @Test
    void aLongErrorIsCutToTheLimitOfTheColumnEndingInDots() {
        String error = "x".repeat(JournalSync.MAX_ERROR_LENGTH + 200);

        JournalSync sync = JournalSync.pending(null, CLOSING, error, FIRST);

        assertThat(sync.lastError()).hasSize(JournalSync.MAX_ERROR_LENGTH).endsWith("...");
    }

    @Test
    void anErrorOfExactlyTheLimitIsKeptWhole() {
        String error = "y".repeat(JournalSync.MAX_ERROR_LENGTH);

        JournalSync sync = JournalSync.pending(null, CLOSING, error, FIRST);

        assertThat(sync.lastError()).isEqualTo(error);
    }

    @Test
    void theLimitIsTheSizeOfTheDatabaseColumn() {
        // incentive_journal_sync.last_error is VARCHAR(500)
        assertThat(JournalSync.MAX_ERROR_LENGTH).isEqualTo(500);
    }
}
