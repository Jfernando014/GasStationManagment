package com.edu.unicauca.gasstation.backend.sales.domain.reconciliation;

/** How a Redeban row was (or was not) attributed to a vendor. */
public enum AssignmentStatus {
    /** Exactly one vendor works at that hour. */
    ASSIGNED,
    /** The row has no time, so the shift cannot decide. Counted in the day total only. */
    NO_TIME,
    /** No vendor of the day covers that hour. */
    NO_SHIFT,
    /** Several vendors cover that hour (for example a titular and a support). Not guessed. */
    AMBIGUOUS
}
