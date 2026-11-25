package com.edu.unicauca.gasstation.backend.sales.domain.services;

import com.edu.unicauca.gasstation.backend.sales.domain.Journal;
import com.edu.unicauca.gasstation.backend.sales.domain.JournalCellNote;
import com.edu.unicauca.gasstation.backend.sales.domain.JournalRepository;
import com.edu.unicauca.gasstation.backend.sales.domain.JournalStatus;
import com.edu.unicauca.gasstation.backend.sales.domain.repositories.JournalCellNoteRepository;
import com.edu.unicauca.gasstation.backend.sales.exception.JournalNotFoundException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Manages Excel-style cell notes attached to specific (step, column, field) cells of a journal (SR-08).
 */
@Service
public class JournalCellNoteService {

    public static final int MAX_NOTE_LENGTH = 500;
    public static final int MAX_KEY_LENGTH = 64;

    private final JournalRepository journalRepository;
    private final JournalCellNoteRepository noteRepository;

    public JournalCellNoteService(
            JournalRepository journalRepository,
            JournalCellNoteRepository noteRepository) {
        this.journalRepository = journalRepository;
        this.noteRepository = noteRepository;
    }

    @Transactional(readOnly = true)
    public List<JournalCellNote> listNotes(Long journalId, Integer stepNumber) {
        requireJournal(journalId);
        if (stepNumber == null) {
            return noteRepository.findAllByJournalId(journalId);
        }
        validateStepNumber(stepNumber);
        return noteRepository.findAllByJournalIdAndStepNumber(journalId, stepNumber);
    }

    @Transactional
    public Optional<JournalCellNote> upsertOrDelete(
            Long journalId,
            Integer stepNumber,
            String rawColumnKey,
            String rawFieldKey,
            String rawNote) {
        Journal journal = requireJournal(journalId);
        if (journal.getStatus() == JournalStatus.CLOSED) {
            throw new IllegalStateException(
                    "La jornada del " + journal.getDate() + " ya está cerrada y no permite modificar notas de celdas.");
        }

        validateStepNumber(stepNumber);
        String columnKey = requireKey(rawColumnKey, "La columna destino es obligatoria.");
        String fieldKey = requireKey(rawFieldKey, "El campo destino es obligatorio.");

        String normalizedNote = rawNote == null ? null : rawNote.trim();
        if (normalizedNote == null || normalizedNote.isEmpty()) {
            noteRepository.deleteByTarget(journalId, stepNumber, columnKey, fieldKey);
            return Optional.empty();
        }
        if (normalizedNote.length() > MAX_NOTE_LENGTH) {
            throw new IllegalArgumentException(
                    "La nota de celda no puede superar los " + MAX_NOTE_LENGTH + " caracteres.");
        }

        JournalCellNote saved = noteRepository.save(new JournalCellNote(
                null,
                journalId,
                stepNumber,
                columnKey,
                fieldKey,
                normalizedNote,
                LocalDateTime.now()));
        return Optional.of(saved);
    }

    private Journal requireJournal(Long journalId) {
        return journalRepository.findById(journalId)
                .orElseThrow(() -> new JournalNotFoundException(journalId));
    }

    private static void validateStepNumber(Integer stepNumber) {
        if (stepNumber == null || stepNumber < 1 || stepNumber > 6) {
            throw new IllegalArgumentException("El número de paso debe estar entre 1 y 6.");
        }
    }

    private static String requireKey(String raw, String requiredMessage) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException(requiredMessage);
        }
        String trimmed = raw.trim();
        if (trimmed.length() > MAX_KEY_LENGTH) {
            throw new IllegalArgumentException(
                    "La clave de celda no puede superar los " + MAX_KEY_LENGTH + " caracteres.");
        }
        return trimmed;
    }
}
