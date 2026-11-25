package com.edu.unicauca.gasstation.backend.sales.domain.repositories;

import com.edu.unicauca.gasstation.backend.sales.domain.JournalCellNote;
import java.util.List;
import java.util.Optional;

public interface JournalCellNoteRepository {

    List<JournalCellNote> findAllByJournalId(Long journalId);

    List<JournalCellNote> findAllByJournalIdAndStepNumber(Long journalId, Integer stepNumber);

    Optional<JournalCellNote> findByTarget(Long journalId, Integer stepNumber, String columnKey, String fieldKey);

    JournalCellNote save(JournalCellNote note);

    void deleteByTarget(Long journalId, Integer stepNumber, String columnKey, String fieldKey);
}
