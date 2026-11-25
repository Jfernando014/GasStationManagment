package com.edu.unicauca.gasstation.backend.sales.domain.services;

import com.edu.unicauca.gasstation.backend.sales.domain.Journal;
import com.edu.unicauca.gasstation.backend.sales.domain.JournalRepository;
import com.edu.unicauca.gasstation.backend.sales.domain.JournalStatus;
import com.edu.unicauca.gasstation.backend.sales.domain.JournalStep;
import com.edu.unicauca.gasstation.backend.sales.domain.StepStatus;
import com.edu.unicauca.gasstation.backend.sales.exception.JournalNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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

    @Test
    @DisplayName("Should save trimmed observations on an open journal")
    void updatesObservationsOnOpenJournal() {
        Journal openJournal = Journal.builder()
                .id(10L)
                .date(LocalDate.of(2026, 11, 24))
                .consecutive(300L)
                .status(JournalStatus.OPEN)
                .build();
        when(journalRepository.findById(10L)).thenReturn(Optional.of(openJournal));
        when(journalRepository.save(any(Journal.class))).thenAnswer(inv -> inv.getArgument(0));

        Journal saved = journalService.updateObservations(10L, "  Surtidor 2 calibrado en la mañana.  ");

        assertThat(saved.getObservations()).isEqualTo("Surtidor 2 calibrado en la mañana.");
        verify(journalRepository).save(openJournal);
    }

    @Test
    @DisplayName("Should clear observations to null when blank text is sent")
    void clearsObservationsWhenBlank() {
        Journal openJournal = Journal.builder()
                .id(10L)
                .date(LocalDate.of(2026, 11, 24))
                .consecutive(300L)
                .status(JournalStatus.OPEN)
                .observations("Nota previa")
                .build();
        when(journalRepository.findById(10L)).thenReturn(Optional.of(openJournal));
        when(journalRepository.save(any(Journal.class))).thenAnswer(inv -> inv.getArgument(0));

        Journal saved = journalService.updateObservations(10L, "   ");

        assertThat(saved.getObservations()).isNull();
    }

    @Test
    @DisplayName("Should reject updating observations on a closed journal")
    void rejectsObservationsUpdateOnClosedJournal() {
        Journal closedJournal = Journal.builder()
                .id(10L)
                .date(LocalDate.of(2026, 11, 24))
                .consecutive(300L)
                .status(JournalStatus.CLOSED)
                .build();
        when(journalRepository.findById(10L)).thenReturn(Optional.of(closedJournal));

        assertThatThrownBy(() -> journalService.updateObservations(10L, "Intento tardío"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("cerrada");
        verify(journalRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should reject observations exceeding 2000 characters")
    void rejectsTooLongObservations() {
        Journal openJournal = Journal.builder()
                .id(10L)
                .date(LocalDate.of(2026, 11, 24))
                .consecutive(300L)
                .status(JournalStatus.OPEN)
                .build();
        when(journalRepository.findById(10L)).thenReturn(Optional.of(openJournal));

        String oversized = "a".repeat(JournalService.MAX_OBSERVATIONS_LENGTH + 1);
        assertThatThrownBy(() -> journalService.updateObservations(10L, oversized))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("2000");
        verify(journalRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw JournalNotFoundException when updating observations of unknown journal")
    void rejectsUnknownJournalOnObservationsUpdate() {
        when(journalRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> journalService.updateObservations(999L, "Nota"))
                .isInstanceOf(JournalNotFoundException.class);
    }

    @Test
    @DisplayName("Should save trimmed observations on a specific step of an open journal")
    void updatesStepObservationsOnOpenJournal() {
        JournalStep step2 = JournalStep.builder()
                .id(20L)
                .stepNumber(2)
                .name("Cierre por vendedor")
                .status(StepStatus.PENDING)
                .build();
        Journal openJournal = Journal.builder()
                .id(10L)
                .date(LocalDate.of(2026, 11, 24))
                .consecutive(300L)
                .status(JournalStatus.OPEN)
                .steps(List.of(step2))
                .build();
        when(journalRepository.findById(10L)).thenReturn(Optional.of(openJournal));
        when(journalRepository.save(any(Journal.class))).thenAnswer(inv -> inv.getArgument(0));

        Journal saved = journalService.updateStepObservations(10L, 2, "  Turno de apoyo entró a las 10:00.  ");

        assertThat(saved.getSteps().get(0).getObservations()).isEqualTo("Turno de apoyo entró a las 10:00.");
        verify(journalRepository).save(openJournal);
    }

    @Test
    @DisplayName("Should clear step observations to null when blank text is sent")
    void clearsStepObservationsWhenBlank() {
        JournalStep step3 = JournalStep.builder()
                .id(30L)
                .stepNumber(3)
                .name("Medios electrónicos")
                .status(StepStatus.PENDING)
                .observations("Nota previa")
                .build();
        Journal openJournal = Journal.builder()
                .id(10L)
                .date(LocalDate.of(2026, 11, 24))
                .consecutive(300L)
                .status(JournalStatus.OPEN)
                .steps(List.of(step3))
                .build();
        when(journalRepository.findById(10L)).thenReturn(Optional.of(openJournal));
        when(journalRepository.save(any(Journal.class))).thenAnswer(inv -> inv.getArgument(0));

        Journal saved = journalService.updateStepObservations(10L, 3, "   ");

        assertThat(saved.getSteps().get(0).getObservations()).isNull();
    }

    @Test
    @DisplayName("Should reject step observations on closed journal or out-of-range step")
    void rejectsStepObservationsWhenClosedOrInvalidStep() {
        Journal closedJournal = Journal.builder()
                .id(10L)
                .date(LocalDate.of(2026, 11, 24))
                .consecutive(300L)
                .status(JournalStatus.CLOSED)
                .steps(List.of())
                .build();
        when(journalRepository.findById(10L)).thenReturn(Optional.of(closedJournal));

        assertThatThrownBy(() -> journalService.updateStepObservations(10L, 2, "Nota tardía"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("cerrada");

        assertThatThrownBy(() -> journalService.updateStepObservations(10L, 7, "Nota"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("1 y 6");
    }
}
