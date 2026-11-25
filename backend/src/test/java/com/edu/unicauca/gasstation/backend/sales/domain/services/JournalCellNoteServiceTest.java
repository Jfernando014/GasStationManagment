package com.edu.unicauca.gasstation.backend.sales.domain.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.edu.unicauca.gasstation.backend.sales.domain.Journal;
import com.edu.unicauca.gasstation.backend.sales.domain.JournalCellNote;
import com.edu.unicauca.gasstation.backend.sales.domain.JournalRepository;
import com.edu.unicauca.gasstation.backend.sales.domain.JournalStatus;
import com.edu.unicauca.gasstation.backend.sales.domain.repositories.JournalCellNoteRepository;
import com.edu.unicauca.gasstation.backend.sales.exception.JournalNotFoundException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("JournalCellNoteService - Excel-style Cell Notes (SR-08)")
class JournalCellNoteServiceTest {

    @Mock
    private JournalRepository journalRepository;

    @Mock
    private JournalCellNoteRepository noteRepository;

    @InjectMocks
    private JournalCellNoteService service;

    private static Journal openJournal(Long id) {
        return Journal.builder()
                .id(id)
                .date(LocalDate.of(2026, 11, 24))
                .consecutive(301L)
                .status(JournalStatus.OPEN)
                .build();
    }

    @Test
    @DisplayName("Should list all cell notes for a journal when stepNumber is omitted")
    void listsAllNotesForJournal() {
        when(journalRepository.findById(10L)).thenReturn(Optional.of(openJournal(10L)));
        JournalCellNote note = new JournalCellNote(
                1L, 10L, 2, "1001", "parciales", "Billete de $50.000 dañado", LocalDateTime.now());
        when(noteRepository.findAllByJournalId(10L)).thenReturn(List.of(note));

        List<JournalCellNote> result = service.listNotes(10L, null);

        assertThat(result).containsExactly(note);
        verify(noteRepository).findAllByJournalId(10L);
    }

    @Test
    @DisplayName("Should list cell notes filtered by stepNumber when specified")
    void listsNotesByStepNumber() {
        when(journalRepository.findById(10L)).thenReturn(Optional.of(openJournal(10L)));
        JournalCellNote note = new JournalCellNote(
                2L, 10L, 2, "1002", "credibanco", "Lote 45 duplicado y anulado", LocalDateTime.now());
        when(noteRepository.findAllByJournalIdAndStepNumber(10L, 2)).thenReturn(List.of(note));

        List<JournalCellNote> result = service.listNotes(10L, 2);

        assertThat(result).containsExactly(note);
        verify(noteRepository).findAllByJournalIdAndStepNumber(10L, 2);
    }

    @Test
    @DisplayName("Should save trimmed cell note on an open journal")
    void upsertsTrimmedNoteOnOpenJournal() {
        when(journalRepository.findById(10L)).thenReturn(Optional.of(openJournal(10L)));
        when(noteRepository.save(any(JournalCellNote.class))).thenAnswer(inv -> {
            JournalCellNote input = inv.getArgument(0);
            return new JournalCellNote(99L, input.journalId(), input.stepNumber(),
                    input.columnKey(), input.fieldKey(), input.note(), input.updatedAt());
        });

        Optional<JournalCellNote> saved = service.upsertOrDelete(
                10L, 2, " 1001 ", " parciales ", "  Entrega parcial verificada con recibo #14  ");

        assertThat(saved).isPresent();
        assertThat(saved.get().id()).isEqualTo(99L);
        assertThat(saved.get().columnKey()).isEqualTo("1001");
        assertThat(saved.get().fieldKey()).isEqualTo("parciales");
        assertThat(saved.get().note()).isEqualTo("Entrega parcial verificada con recibo #14");
    }

    @Test
    @DisplayName("Should delete cell note and return empty when blank text is sent")
    void deletesNoteWhenBlank() {
        when(journalRepository.findById(10L)).thenReturn(Optional.of(openJournal(10L)));

        Optional<JournalCellNote> result = service.upsertOrDelete(10L, 2, "1001", "pico", "   ");

        assertThat(result).isEmpty();
        verify(noteRepository).deleteByTarget(10L, 2, "1001", "pico");
        verify(noteRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should reject modifying cell notes on a closed journal")
    void rejectsClosedJournal() {
        Journal closed = Journal.builder()
                .id(10L)
                .date(LocalDate.of(2026, 11, 24))
                .consecutive(301L)
                .status(JournalStatus.CLOSED)
                .build();
        when(journalRepository.findById(10L)).thenReturn(Optional.of(closed));

        assertThatThrownBy(() -> service.upsertOrDelete(10L, 2, "1001", "qr", "Transferencia tardía"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("cerrada");
        verify(noteRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should reject cell note exceeding 500 characters")
    void rejectsOversizedNote() {
        when(journalRepository.findById(10L)).thenReturn(Optional.of(openJournal(10L)));
        String tooLong = "x".repeat(JournalCellNoteService.MAX_NOTE_LENGTH + 1);

        assertThatThrownBy(() -> service.upsertOrDelete(10L, 2, "1001", "descuadre", tooLong))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("500");
        verify(noteRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should reject invalid step number and blank keys")
    void rejectsInvalidStepAndKeys() {
        when(journalRepository.findById(10L)).thenReturn(Optional.of(openJournal(10L)));

        assertThatThrownBy(() -> service.upsertOrDelete(10L, 7, "1001", "ga", "Nota"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("1 y 6");
        assertThatThrownBy(() -> service.upsertOrDelete(10L, 2, "  ", "ga", "Nota"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("columna");
        assertThatThrownBy(() -> service.upsertOrDelete(10L, 2, "1001", "", "Nota"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("campo");
    }

    @Test
    @DisplayName("Should throw JournalNotFoundException when journal does not exist")
    void rejectsUnknownJournal() {
        when(journalRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.listNotes(999L, 2))
                .isInstanceOf(JournalNotFoundException.class);
    }
}
