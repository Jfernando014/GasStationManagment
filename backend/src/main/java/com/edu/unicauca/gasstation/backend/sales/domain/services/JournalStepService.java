package com.edu.unicauca.gasstation.backend.sales.domain.services;

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

        JournalStep step = journalStepRepository.findByJournalIdAndStepNumber(journalId, stepNumber)
                .orElseThrow(() -> new IllegalArgumentException("Paso no encontrado"));

        step.setStatus(StepStatus.COMPLETED);
        step.setCompletedAt(LocalDateTime.now());
        
        return journalStepRepository.save(step);
    }

    public boolean isJournalEditable(Long journalId) {
        // En el futuro: si el paso 6 (cierre) está completo, ya no es editable
        return journalStepRepository.findByJournalIdAndStepNumber(journalId, 6)
                .map(step -> step.getStatus() != StepStatus.COMPLETED)
                .orElse(false);
    }

    private void checkStepCompleted(Long journalId, Integer stepNumber) {
        JournalStep step = journalStepRepository.findByJournalIdAndStepNumber(journalId, stepNumber)
                .orElseThrow(() -> new IllegalArgumentException("Paso " + stepNumber + " no encontrado"));
        if (step.getStatus() != StepStatus.COMPLETED) {
            throw new IllegalStateException("No se puede avanzar: el paso " + stepNumber + " no está completado.");
        }
    }
}
