package com.edu.unicauca.gasstation.backend.sales.infrastructure.persistence.adapters;

import com.edu.unicauca.gasstation.backend.sales.domain.Journal;
import com.edu.unicauca.gasstation.backend.sales.domain.JournalRepository;
import com.edu.unicauca.gasstation.backend.sales.domain.JournalStatus;
import com.edu.unicauca.gasstation.backend.sales.domain.JournalStep;
import com.edu.unicauca.gasstation.backend.sales.domain.StepStatus;
import com.edu.unicauca.gasstation.backend.sales.domain.services.JournalService;
import com.edu.unicauca.gasstation.backend.shifts.daystaff.DayStaff.Step1Status;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("DayJournalAdapter - Secondary adapter satisfying shifts DayJournalPort")
class DayJournalAdapterTest {

    @Mock
    private JournalRepository journalRepository;

    @Mock
    private JournalService journalService;

    @InjectMocks
    private DayJournalAdapter adapter;

    private LocalDate date;
    private Journal journal;
    private JournalStep step1;
    private JournalStep step2;

    @BeforeEach
    void setUp() {
        date = LocalDate.of(2026, 8, 5);
        step1 = JournalStep.builder()
                .stepNumber(1)
                .name("Medición inicial")
                .status(StepStatus.PENDING)
                .build();
        step2 = JournalStep.builder()
                .stepNumber(2)
                .name("Asignación de turnos")
                .status(StepStatus.PENDING)
                .build();

        journal = Journal.builder()
                .id(10L)
                .date(date)
                .consecutive(5L)
                .status(JournalStatus.OPEN)
                .steps(new ArrayList<>(List.of(step1, step2)))
                .build();
    }

    @Test
    @DisplayName("Returns NO_JOURNAL when no journal exists for the given date")
    void returnsNoJournalWhenMissing() {
        when(journalRepository.findByDate(date)).thenReturn(Optional.empty());

        assertThat(adapter.step1Status(date)).isEqualTo(Step1Status.NO_JOURNAL);
    }

    @Test
    @DisplayName("Returns PENDING when step 1 is pending")
    void returnsPendingWhenStep1Pending() {
        when(journalRepository.findByDate(date)).thenReturn(Optional.of(journal));

        assertThat(adapter.step1Status(date)).isEqualTo(Step1Status.PENDING);
    }

    @Test
    @DisplayName("Returns CONFIRMED when step 1 is completed")
    void returnsConfirmedWhenStep1Completed() {
        step1.setStatus(StepStatus.COMPLETED);
        when(journalRepository.findByDate(date)).thenReturn(Optional.of(journal));

        assertThat(adapter.step1Status(date)).isEqualTo(Step1Status.CONFIRMED);
    }

    @Test
    @DisplayName("Returns true for isEditable when journal is OPEN or does not exist yet")
    void isEditableReturnsTrueWhenOpenOrMissing() {
        when(journalRepository.findByDate(date)).thenReturn(Optional.empty());
        assertThat(adapter.isEditable(date)).isTrue();

        when(journalRepository.findByDate(date)).thenReturn(Optional.of(journal));
        assertThat(adapter.isEditable(date)).isTrue();
    }

    @Test
    @DisplayName("Returns false for isEditable when journal is CLOSED")
    void isEditableReturnsFalseWhenClosed() {
        journal.setStatus(JournalStatus.CLOSED);
        when(journalRepository.findByDate(date)).thenReturn(Optional.of(journal));

        assertThat(adapter.isEditable(date)).isFalse();
    }

    @Test
    @DisplayName("Marks step 1 as completed and advances step 2 to in progress")
    void marksStep1CompletedSuccessfully() {
        when(journalRepository.findByDate(date)).thenReturn(Optional.of(journal));
        when(journalService.getOrCreateJournal(date)).thenReturn(journal);

        adapter.markStep1Completed(date);

        assertThat(step1.getStatus()).isEqualTo(StepStatus.COMPLETED);
        assertThat(step1.getCompletedAt()).isNotNull();
        assertThat(step2.getStatus()).isEqualTo(StepStatus.IN_PROGRESS);
        verify(journalRepository).save(journal);
    }

    @Test
    @DisplayName("Throws IllegalStateException when attempting to mark completed on closed journal")
    void throwsWhenJournalIsClosed() {
        journal.setStatus(JournalStatus.CLOSED);
        when(journalRepository.findByDate(date)).thenReturn(Optional.of(journal));

        assertThatThrownBy(() -> adapter.markStep1Completed(date))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("closed");
    }
}
