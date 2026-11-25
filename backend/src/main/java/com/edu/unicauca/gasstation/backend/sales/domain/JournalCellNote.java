package com.edu.unicauca.gasstation.backend.sales.domain;

import java.time.LocalDateTime;

/**
 * Pure domain representation of an Excel-style note attached to a specific cell of a journal step
 * (e.g. a vendor column and concept field on Step 2).
 */
public record JournalCellNote(
        Long id,
        Long journalId,
        Integer stepNumber,
        String columnKey,
        String fieldKey,
        String note,
        LocalDateTime updatedAt) {
}
