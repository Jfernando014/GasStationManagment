package com.edu.unicauca.gasstation.backend.sales.domain.services;

import com.edu.unicauca.gasstation.backend.sales.domain.Journal;
import com.edu.unicauca.gasstation.backend.sales.domain.JournalRepository;
import com.edu.unicauca.gasstation.backend.sales.domain.JournalStatus;
import com.edu.unicauca.gasstation.backend.sales.domain.JournalStep;
import com.edu.unicauca.gasstation.backend.sales.domain.StepStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("JournalService - Daily Journal Management (S1-13)")
class JournalServiceTest {

    @Mock
    private JournalRepository journalRepository;

    @InjectMocks
    private JournalService journalService;

    @Test
    @DisplayName("Should create new journal with consecutive and 6 pending steps when date does not exist")
    void createsNewJournalWhenNoneExistsForGivenDate() {
        LocalDate date = LocalDate.of(2026, 8, 5);
        when(journalRepository.findByDate(date)).thenReturn(Optional.empty());
        when(journalRepository.findMaxConsecutive()).thenReturn(Optional.of(42L));
        when(journalRepository.save(any(Journal.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Journal result = journalService.getOrCreateJournal(date);

        assertThat(result).isNotNull();
        assertThat(result.getDate()).isEqualTo(date);
        assertThat(result.getConsecutive()).isEqualTo(43L);
        assertThat(result.getStatus()).isEqualTo(JournalStatus.OPEN);

        List<JournalStep> steps = result.getSteps();
        assertThat(steps).hasSize(6);

        String[] expectedNames = {
                "Medición inicial",
                "Asignación de turnos",
                "Apertura de surtidores",
                "Registro de lecturas",
                "Cierre de surtidores",
                "Medición final y cuadre"
        };

        for (int i = 0; i < 6; i++) {
            JournalStep step = steps.get(i);
            assertThat(step.getStepNumber()).isEqualTo(i + 1);
            assertThat(step.getName()).isEqualTo(expectedNames[i]);
            assertThat(step.getStatus()).isEqualTo(StepStatus.PENDING);
            assertThat(step.getJournal()).isSameAs(result);
        }

        verify(journalRepository).findByDate(date);
        verify(journalRepository).findMaxConsecutive();
        verify(journalRepository).save(any(Journal.class));
    }

    @Test
    @DisplayName("Should assign consecutive 1 when no previous journals exist")
    void assignsConsecutiveOneWhenNoPreviousJournalsExist() {
        LocalDate date = LocalDate.of(2026, 8, 5);
        when(journalRepository.findByDate(date)).thenReturn(Optional.empty());
        when(journalRepository.findMaxConsecutive()).thenReturn(Optional.empty());
        when(journalRepository.save(any(Journal.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Journal result = journalService.getOrCreateJournal(date);

        assertThat(result.getConsecutive()).isEqualTo(1L);
        assertThat(result.getStatus()).isEqualTo(JournalStatus.OPEN);
        verify(journalRepository).save(any(Journal.class));
    }

    @Test
    @DisplayName("Should return existing journal without duplicating when date already has a journal")
    void returnsExistingJournalWithoutDuplicatingWhenAlreadyCreated() {
        LocalDate date = LocalDate.of(2026, 8, 5);
        Journal existing = Journal.builder()
                .id(100L)
                .date(date)
                .consecutive(5L)
                .status(JournalStatus.OPEN)
                .build();

        when(journalRepository.findByDate(date)).thenReturn(Optional.of(existing));

        Journal result = journalService.getOrCreateJournal(date);

        assertThat(result).isSameAs(existing);
        assertThat(result.getId()).isEqualTo(100L);
        assertThat(result.getConsecutive()).isEqualTo(5L);

        verify(journalRepository).findByDate(date);
        verify(journalRepository, never()).findMaxConsecutive();
        verify(journalRepository, never()).save(any());
    }
}
