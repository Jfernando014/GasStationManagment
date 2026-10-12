package com.edu.unicauca.gasstation.backend.sales.domain.services;

import com.edu.unicauca.gasstation.backend.sales.domain.JournalStep;
import com.edu.unicauca.gasstation.backend.sales.domain.JournalStepRepository;
import com.edu.unicauca.gasstation.backend.sales.domain.StepStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("JournalStepService - Step Completion and Validation Rules (S1-14)")
class JournalStepServiceTest {

    @Mock
    private JournalStepRepository journalStepRepository;

    @InjectMocks
    private JournalStepService journalStepService;

    @Test
    @DisplayName("Should mark regular step as completed with timestamp")
    void marksRegularStepAsCompletedSuccessfully() {
        Long journalId = 1L;
        Integer stepNumber = 2;

        JournalStep step2 = JournalStep.builder()
                .id(20L)
                .stepNumber(stepNumber)
                .name("Asignación de turnos")
                .status(StepStatus.PENDING)
                .build();

        when(journalStepRepository.findByJournalIdAndStepNumber(journalId, stepNumber))
                .thenReturn(Optional.of(step2));
        when(journalStepRepository.save(any(JournalStep.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        JournalStep result = journalStepService.markStepAsCompleted(journalId, stepNumber);

        assertThat(result.getStatus()).isEqualTo(StepStatus.COMPLETED);
        assertThat(result.getCompletedAt()).isNotNull();
        verify(journalStepRepository).save(step2);
    }

    @Test
    @DisplayName("Should throw IllegalStateException when completing step 4 if step 2 is not completed")
    void throwsExceptionWhenStep4CompletedWithoutStep2() {
        Long journalId = 1L;

        JournalStep step2 = JournalStep.builder()
                .id(20L)
                .stepNumber(2)
                .status(StepStatus.PENDING)
                .build();

        when(journalStepRepository.findByJournalIdAndStepNumber(journalId, 2))
                .thenReturn(Optional.of(step2));

        assertThatThrownBy(() -> journalStepService.markStepAsCompleted(journalId, 4))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("el paso 2 no está completado");

        verify(journalStepRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw IllegalStateException when completing step 4 if step 3 is not completed")
    void throwsExceptionWhenStep4CompletedWithoutStep3() {
        Long journalId = 1L;

        JournalStep step2 = JournalStep.builder()
                .id(20L)
                .stepNumber(2)
                .status(StepStatus.COMPLETED)
                .build();

        JournalStep step3 = JournalStep.builder()
                .id(30L)
                .stepNumber(3)
                .status(StepStatus.PENDING)
                .build();

        when(journalStepRepository.findByJournalIdAndStepNumber(journalId, 2))
                .thenReturn(Optional.of(step2));
        when(journalStepRepository.findByJournalIdAndStepNumber(journalId, 3))
                .thenReturn(Optional.of(step3));

        assertThatThrownBy(() -> journalStepService.markStepAsCompleted(journalId, 4))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("el paso 3 no está completado");

        verify(journalStepRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should mark step 4 as completed when both steps 2 and 3 are already completed")
    void marksStep4CompletedWhenPrerequisitesMet() {
        Long journalId = 1L;

        JournalStep step2 = JournalStep.builder()
                .id(20L)
                .stepNumber(2)
                .status(StepStatus.COMPLETED)
                .build();

        JournalStep step3 = JournalStep.builder()
                .id(30L)
                .stepNumber(3)
                .status(StepStatus.COMPLETED)
                .build();

        JournalStep step4 = JournalStep.builder()
                .id(40L)
                .stepNumber(4)
                .status(StepStatus.PENDING)
                .build();

        when(journalStepRepository.findByJournalIdAndStepNumber(journalId, 2))
                .thenReturn(Optional.of(step2));
        when(journalStepRepository.findByJournalIdAndStepNumber(journalId, 3))
                .thenReturn(Optional.of(step3));
        when(journalStepRepository.findByJournalIdAndStepNumber(journalId, 4))
                .thenReturn(Optional.of(step4));
        when(journalStepRepository.save(any(JournalStep.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        JournalStep result = journalStepService.markStepAsCompleted(journalId, 4);

        assertThat(result.getStatus()).isEqualTo(StepStatus.COMPLETED);
        assertThat(result.getCompletedAt()).isNotNull();
        verify(journalStepRepository).save(step4);
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when step to complete does not exist")
    void throwsExceptionWhenTargetStepNotFound() {
        Long journalId = 1L;
        Integer stepNumber = 5;

        when(journalStepRepository.findByJournalIdAndStepNumber(journalId, stepNumber))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> journalStepService.markStepAsCompleted(journalId, stepNumber))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Paso no encontrado");
    }

    @Test
    @DisplayName("Should return true for isJournalEditable when step 6 is still pending")
    void returnsTrueWhenStep6IsPending() {
        Long journalId = 1L;

        JournalStep step6 = JournalStep.builder()
                .id(60L)
                .stepNumber(6)
                .status(StepStatus.PENDING)
                .build();

        when(journalStepRepository.findByJournalIdAndStepNumber(journalId, 6))
                .thenReturn(Optional.of(step6));

        boolean editable = journalStepService.isJournalEditable(journalId);

        assertThat(editable).isTrue();
    }

    @Test
    @DisplayName("Should return false for isJournalEditable when step 6 is completed")
    void returnsFalseWhenStep6IsCompleted() {
        Long journalId = 1L;

        JournalStep step6 = JournalStep.builder()
                .id(60L)
                .stepNumber(6)
                .status(StepStatus.COMPLETED)
                .build();

        when(journalStepRepository.findByJournalIdAndStepNumber(journalId, 6))
                .thenReturn(Optional.of(step6));

        boolean editable = journalStepService.isJournalEditable(journalId);

        assertThat(editable).isFalse();
    }

    @Test
    @DisplayName("Should return false for isJournalEditable when step 6 is not found")
    void returnsFalseWhenStep6NotFound() {
        Long journalId = 999L;

        when(journalStepRepository.findByJournalIdAndStepNumber(journalId, 6))
                .thenReturn(Optional.empty());

        boolean editable = journalStepService.isJournalEditable(journalId);

        assertThat(editable).isFalse();
    }
}
