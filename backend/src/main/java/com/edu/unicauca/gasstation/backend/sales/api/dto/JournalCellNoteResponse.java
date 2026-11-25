package com.edu.unicauca.gasstation.backend.sales.api.dto;

import com.edu.unicauca.gasstation.backend.sales.domain.JournalCellNote;
import java.time.LocalDateTime;

/**
 * Response payload for an Excel-style cell note attached to a journal grid cell (SR-08).
 */
public record JournalCellNoteResponse(
        Long id,
        Long journalId,
        Integer stepNumber,
        String columnKey,
        String fieldKey,
        String note,
        LocalDateTime updatedAt) {

    public static JournalCellNoteResponse from(JournalCellNote cellNote) {
        return new JournalCellNoteResponse(
                cellNote.id(),
                cellNote.journalId(),
                cellNote.stepNumber(),
                cellNote.columnKey(),
                cellNote.fieldKey(),
                cellNote.note(),
                cellNote.updatedAt());
    }
}
