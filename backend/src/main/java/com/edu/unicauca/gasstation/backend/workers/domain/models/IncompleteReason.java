package com.edu.unicauca.gasstation.backend.workers.domain.models;

/** Why the hours of a day cannot be trusted in a fortnight sheet. */
public enum IncompleteReason {
    /** The day has no processing state: its journal was not closed, or closed before the hours were kept. */
    MISSING_RECORD,
    /** Keeping the hours of the day failed and it is waiting to be reprocessed. */
    PENDING,
    /** The day was kept from the grid because the novelties could not be applied. */
    NOVELTIES_NOT_APPLIED
}
