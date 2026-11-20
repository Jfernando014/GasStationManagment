package com.edu.unicauca.gasstation.backend.inventory;

import java.time.LocalDate;

/**
 * Contract owned by Inventory and implemented by the Sales module, which owns the journal and its steps.
 * It lets Inventory reset its own step without knowing how the journal is stored.
 */
public interface JournalStepPort {

    /** @return true when the journal of the date is closed and cannot be modified; false if it is open or does not exist */
    boolean isJournalClosed(LocalDate journalDate);

    /** Returns the step of the journal to PENDING without a completion time; does nothing if the journal does not exist. */
    void returnStepToPending(LocalDate journalDate, int stepNumber);
}
