package com.edu.unicauca.gasstation.backend.sales.domain.services;

import com.edu.unicauca.gasstation.backend.sales.domain.Journal;
import com.edu.unicauca.gasstation.backend.sales.domain.JournalStatus;
import com.edu.unicauca.gasstation.backend.sales.domain.StepStatus;
import java.time.LocalDate;

/** Rules shared by the reset of a journal step: a closed journal cannot be reset and the step goes back to PENDING. */
public final class JournalStepReset {

    private static final int CLOSING_STEP = 6;

    private JournalStepReset() {
    }

    public static boolean isClosed(Journal journal) {
        if (journal.getStatus() == JournalStatus.CLOSED) {
            return true;
        }
        return journal.getSteps() != null && journal.getSteps().stream()
                .anyMatch(s -> s.getStepNumber() != null && s.getStepNumber() == CLOSING_STEP
                        && s.getStatus() == StepStatus.COMPLETED);
    }

    public static String closedMessage(LocalDate date) {
        return "La jornada del " + date + " ya está cerrada y no permite modificaciones.";
    }

    /** Leaves only the given step as PENDING without a completion time; true when something changed. */
    public static boolean returnToPending(Journal journal, int stepNumber) {
        if (journal.getSteps() == null) {
            return false;
        }
        boolean changed = false;
        for (var step : journal.getSteps()) {
            if (step.getStepNumber() != null && step.getStepNumber() == stepNumber) {
                step.setStatus(StepStatus.PENDING);
                step.setCompletedAt(null);
                changed = true;
            }
        }
        return changed;
    }
}
