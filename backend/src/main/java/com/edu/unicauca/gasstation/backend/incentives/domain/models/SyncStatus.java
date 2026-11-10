package com.edu.unicauca.gasstation.backend.incentives.domain.models;

/** Whether the additive sales of a journal date are stored ({@link #PROCESSED}) or still have to be ({@link #PENDING}). */
public enum SyncStatus {
    PROCESSED,
    PENDING
}
