package com.edu.unicauca.gasstation.backend.sales.infrastructure.persistence.adapters;

import com.edu.unicauca.gasstation.backend.sales.domain.JournalCellNote;
import com.edu.unicauca.gasstation.backend.sales.domain.repositories.JournalCellNoteRepository;
import com.edu.unicauca.gasstation.backend.sales.infrastructure.persistence.entities.JournalCellNoteEntity;
import com.edu.unicauca.gasstation.backend.sales.infrastructure.persistence.repositories.JournalCellNoteJpaRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class JournalCellNoteRepositoryAdapter implements JournalCellNoteRepository {

    private final JournalCellNoteJpaRepository jpaRepository;

    public JournalCellNoteRepositoryAdapter(JournalCellNoteJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<JournalCellNote> findAllByJournalId(Long journalId) {
        return jpaRepository.findAllByJournalIdOrderByStepNumberAscColumnKeyAscFieldKeyAsc(journalId)
                .stream()
                .map(JournalCellNoteRepositoryAdapter::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<JournalCellNote> findAllByJournalIdAndStepNumber(Long journalId, Integer stepNumber) {
        return jpaRepository.findAllByJournalIdAndStepNumberOrderByColumnKeyAscFieldKeyAsc(journalId, stepNumber)
                .stream()
                .map(JournalCellNoteRepositoryAdapter::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<JournalCellNote> findByTarget(
            Long journalId, Integer stepNumber, String columnKey, String fieldKey) {
        return jpaRepository
                .findByJournalIdAndStepNumberAndColumnKeyAndFieldKey(journalId, stepNumber, columnKey, fieldKey)
                .map(JournalCellNoteRepositoryAdapter::toDomain);
    }

    @Override
    @Transactional
    public JournalCellNote save(JournalCellNote note) {
        JournalCellNoteEntity entity = jpaRepository
                .findByJournalIdAndStepNumberAndColumnKeyAndFieldKey(
                        note.journalId(), note.stepNumber(), note.columnKey(), note.fieldKey())
                .map(existing -> {
                    existing.setNote(note.note());
                    existing.setUpdatedAt(note.updatedAt() != null ? note.updatedAt() : LocalDateTime.now());
                    return existing;
                })
                .orElseGet(() -> new JournalCellNoteEntity(
                        null,
                        note.journalId(),
                        note.stepNumber(),
                        note.columnKey(),
                        note.fieldKey(),
                        note.note(),
                        note.updatedAt() != null ? note.updatedAt() : LocalDateTime.now()));

        return toDomain(jpaRepository.save(entity));
    }

    @Override
    @Transactional
    public void deleteByTarget(Long journalId, Integer stepNumber, String columnKey, String fieldKey) {
        jpaRepository.deleteByJournalIdAndStepNumberAndColumnKeyAndFieldKey(
                journalId, stepNumber, columnKey, fieldKey);
    }

    private static JournalCellNote toDomain(JournalCellNoteEntity entity) {
        return new JournalCellNote(
                entity.getId(),
                entity.getJournalId(),
                entity.getStepNumber(),
                entity.getColumnKey(),
                entity.getFieldKey(),
                entity.getNote(),
                entity.getUpdatedAt());
    }
}
