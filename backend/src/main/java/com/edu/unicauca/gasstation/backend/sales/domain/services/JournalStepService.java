package com.edu.unicauca.gasstation.backend.sales.domain.services;

import com.edu.unicauca.gasstation.backend.sales.domain.JournalStatus;
import com.edu.unicauca.gasstation.backend.sales.domain.JournalStep;
import com.edu.unicauca.gasstation.backend.sales.domain.JournalStepRepository;
import com.edu.unicauca.gasstation.backend.sales.domain.StepStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class JournalStepService {

    private final JournalStepRepository journalStepRepository;

    @Transactional
    public JournalStep markStepAsCompleted(Long journalId, Integer stepNumber) {
        // Validación específica: el paso 4 requiere el 2 y el 3
        if (stepNumber == 4) {
            checkStepCompleted(journalId, 2);
            checkStepCompleted(journalId, 3);
        }
        // El paso 5 calcula la variación y el stock con las ventas del paso 2
        if (stepNumber == 5) {
            checkStepCompleted(journalId, 2);
        }

        JournalStep step = journalStepRepository.findByJournalIdAndStepNumber(journalId, stepNumber)
                .orElseThrow(() -> new IllegalArgumentException("Paso no encontrado"));

        if (isJournalClosed(step)) {
            throw new IllegalStateException("La jornada ya está cerrada y no permite modificaciones.");
        }

        step.setStatus(StepStatus.COMPLETED);
        step.setCompletedAt(LocalDateTime.now());
        
        return journalStepRepository.save(step);
    }

    private boolean isJournalClosed(JournalStep step) {
        if (step.getStepNumber() != null && step.getStepNumber() == 6 && step.getStatus() == StepStatus.COMPLETED) {
            return true;
        }
        if (step.getJournal() == null) {
            return false;
        }
        if (step.getJournal().getStatus() == JournalStatus.CLOSED) {
            return true;
        }
        return step.getJournal().getSteps() != null && step.getJournal().getSteps().stream()
                .anyMatch(s -> s.getStepNumber() != null && s.getStepNumber() == 6 && s.getStatus() == StepStatus.COMPLETED);
    }

    public boolean isJournalEditable(Long journalId) {
        return journalStepRepository.findByJournalIdAndStepNumber(journalId, 6)
                .map(step -> step.getStatus() != StepStatus.COMPLETED
                        && (step.getJournal() == null || step.getJournal().getStatus() != JournalStatus.CLOSED))
                .orElse(false);
    }

    public void assertJournalEditable(Long journalId) {
        if (!isJournalEditable(journalId)) {
            throw new IllegalStateException("La jornada ya está cerrada y no permite modificaciones.");
        }
    }

    private void checkStepCompleted(Long journalId, Integer stepNumber) {
        JournalStep step = journalStepRepository.findByJournalIdAndStepNumber(journalId, stepNumber)
                .orElseThrow(() -> new IllegalArgumentException("Paso " + stepNumber + " no encontrado"));
        if (step.getStatus() != StepStatus.COMPLETED) {
            throw new IllegalStateException("No se puede avanzar: el paso " + stepNumber + " no está completado.");
        }
    }
}
