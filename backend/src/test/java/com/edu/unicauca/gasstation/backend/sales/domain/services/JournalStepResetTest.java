package com.edu.unicauca.gasstation.backend.sales.domain.services;

import static org.assertj.core.api.Assertions.assertThat;

import com.edu.unicauca.gasstation.backend.sales.domain.Journal;
import com.edu.unicauca.gasstation.backend.sales.domain.JournalStatus;
import com.edu.unicauca.gasstation.backend.sales.domain.JournalStep;
import com.edu.unicauca.gasstation.backend.sales.domain.StepStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** The rules shared by every reset of a journal step: when a journal counts as closed and how a step goes back. */
class JournalStepResetTest {

    private static final LocalDate DAY = LocalDate.of(2026, 8, 5);
    private static final LocalDateTime DONE_AT = LocalDateTime.of(2026, 8, 5, 20, 15);

    private static JournalStep step(Integer number, StepStatus status) {
        return JournalStep.builder().stepNumber(number).name("Paso " + number).status(status)
                .completedAt(status == StepStatus.COMPLETED ? DONE_AT : null).build();
    }

    private static Journal journal(JournalStatus status, JournalStep... steps) {
        return Journal.builder().id(7L).date(DAY).consecutive(288L).status(status)
                .steps(new ArrayList<>(List.of(steps))).build();
    }

    private static StepStatus statusOf(Journal journal, int number) {
        return journal.getSteps().stream().filter(step -> step.getStepNumber() == number).findFirst().orElseThrow()
                .getStatus();
    }

    // ---- is the journal closed ----

    @Test
    void aJournalWithClosedStatusIsClosedWhateverItsSteps() {
        assertThat(JournalStepReset.isClosed(journal(JournalStatus.CLOSED, step(6, StepStatus.PENDING)))).isTrue();
    }

    @Test
    void anOpenJournalWithTheClosingStepCompletedIsClosedToo() {
        assertThat(JournalStepReset.isClosed(journal(JournalStatus.OPEN, step(5, StepStatus.COMPLETED),
                step(6, StepStatus.COMPLETED)))).isTrue();
    }

    @Test
    void anOpenJournalWhoseClosingStepIsNotCompletedIsOpen() {
        assertThat(JournalStepReset.isClosed(journal(JournalStatus.OPEN, step(5, StepStatus.COMPLETED),
                step(6, StepStatus.PENDING)))).isFalse();
        assertThat(JournalStepReset.isClosed(journal(JournalStatus.OPEN, step(6, StepStatus.IN_PROGRESS)))).isFalse();
    }

    @Test
    void completingAnotherStepDoesNotCloseTheJournal() {
        assertThat(JournalStepReset.isClosed(journal(JournalStatus.OPEN, step(1, StepStatus.COMPLETED),
                step(2, StepStatus.COMPLETED), step(3, StepStatus.COMPLETED), step(4, StepStatus.COMPLETED),
                step(5, StepStatus.COMPLETED)))).isFalse();
    }

    @Test
    void anOpenJournalWithoutStepsIsOpen() {
        Journal withoutSteps = journal(JournalStatus.OPEN);
        withoutSteps.setSteps(null);

        assertThat(JournalStepReset.isClosed(withoutSteps)).isFalse();
        assertThat(JournalStepReset.isClosed(journal(JournalStatus.OPEN))).isFalse();
    }

    @Test
    void aStepWithoutNumberIsIgnoredWhenLookingForTheClosingStep() {
        assertThat(JournalStepReset.isClosed(journal(JournalStatus.OPEN, step(null, StepStatus.COMPLETED)))).isFalse();
    }

    @Test
    void theClosedMessageNamesTheDate() {
        assertThat(JournalStepReset.closedMessage(DAY))
                .isEqualTo("La jornada del 2026-08-05 ya está cerrada y no permite modificaciones.");
    }

    // ---- return a step to pending ----

    @Test
    void theStepGoesBackToPendingWithoutCompletionTimeAndTheAnswerSaysSomethingChanged() {
        Journal journal = journal(JournalStatus.OPEN, step(3, StepStatus.COMPLETED));

        boolean changed = JournalStepReset.returnToPending(journal, 3);

        assertThat(changed).isTrue();
        assertThat(journal.getSteps().get(0).getStatus()).isEqualTo(StepStatus.PENDING);
        assertThat(journal.getSteps().get(0).getCompletedAt()).isNull();
    }

    @Test
    void onlyTheGivenStepChanges() {
        Journal journal = journal(JournalStatus.OPEN, step(2, StepStatus.COMPLETED), step(3, StepStatus.COMPLETED),
                step(4, StepStatus.COMPLETED));

        JournalStepReset.returnToPending(journal, 3);

        assertThat(statusOf(journal, 2)).isEqualTo(StepStatus.COMPLETED);
        assertThat(statusOf(journal, 3)).isEqualTo(StepStatus.PENDING);
        assertThat(statusOf(journal, 4)).isEqualTo(StepStatus.COMPLETED);
        assertThat(journal.getSteps().get(0).getCompletedAt()).isEqualTo(DONE_AT);
        assertThat(journal.getSteps().get(2).getCompletedAt()).isEqualTo(DONE_AT);
    }

    @Test
    void aStepInProgressAlsoGoesBackToPending() {
        Journal journal = journal(JournalStatus.OPEN, step(5, StepStatus.IN_PROGRESS));

        assertThat(JournalStepReset.returnToPending(journal, 5)).isTrue();
        assertThat(statusOf(journal, 5)).isEqualTo(StepStatus.PENDING);
    }

    @Test
    void aStepThatDoesNotExistChangesNothing() {
        Journal journal = journal(JournalStatus.OPEN, step(2, StepStatus.COMPLETED));

        assertThat(JournalStepReset.returnToPending(journal, 5)).isFalse();
        assertThat(statusOf(journal, 2)).isEqualTo(StepStatus.COMPLETED);
    }

    @Test
    void aJournalWithoutStepsChangesNothing() {
        Journal journal = journal(JournalStatus.OPEN);
        journal.setSteps(null);

        assertThat(JournalStepReset.returnToPending(journal, 3)).isFalse();
    }

    @Test
    void stepsWithoutNumberAreSkipped() {
        Journal journal = journal(JournalStatus.OPEN, step(null, StepStatus.COMPLETED), step(3, StepStatus.COMPLETED));

        assertThat(JournalStepReset.returnToPending(journal, 3)).isTrue();
        assertThat(journal.getSteps().get(0).getStatus()).isEqualTo(StepStatus.COMPLETED);
    }
}
