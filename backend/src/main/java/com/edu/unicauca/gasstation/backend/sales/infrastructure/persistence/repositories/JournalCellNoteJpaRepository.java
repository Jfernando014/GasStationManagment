package com.edu.unicauca.gasstation.backend.sales.infrastructure.persistence.repositories;

import com.edu.unicauca.gasstation.backend.sales.infrastructure.persistence.entities.JournalCellNoteEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JournalCellNoteJpaRepository extends JpaRepository<JournalCellNoteEntity, Long> {

    List<JournalCellNoteEntity> findAllByJournalIdOrderByStepNumberAscColumnKeyAscFieldKeyAsc(Long journalId);

    List<JournalCellNoteEntity> findAllByJournalIdAndStepNumberOrderByColumnKeyAscFieldKeyAsc(
            Long journalId, Integer stepNumber);

    Optional<JournalCellNoteEntity> findByJournalIdAndStepNumberAndColumnKeyAndFieldKey(
            Long journalId, Integer stepNumber, String columnKey, String fieldKey);

    void deleteByJournalIdAndStepNumberAndColumnKeyAndFieldKey(
            Long journalId, Integer stepNumber, String columnKey, String fieldKey);
}
