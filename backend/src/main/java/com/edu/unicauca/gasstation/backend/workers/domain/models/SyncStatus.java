package com.edu.unicauca.gasstation.backend.workers.domain.models;

/** Whether the hours of a date are stored ({@link #PROCESSED}) or still have to be ({@link #PENDING}). */
public enum SyncStatus {
    PROCESSED,
    PENDING
}
