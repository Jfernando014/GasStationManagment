package com.edu.unicauca.gasstation.backend.sales.domain.services;

import com.edu.unicauca.gasstation.backend.sales.domain.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class JournalService {

    private final JournalRepository journalRepository;

    @Transactional
    public Journal getOrCreateJournal(LocalDate date) {
        return journalRepository.findByDate(date).orElseGet(() -> createJournal(date));
    }

    private Journal createJournal(LocalDate date) {
        Long maxConsecutive = journalRepository.findMaxConsecutive().orElse(0L);
        Long newConsecutive = maxConsecutive + 1;

        Journal journal = Journal.builder()
                .date(date)
                .consecutive(newConsecutive)
                .status(JournalStatus.OPEN)
                .steps(new ArrayList<>())
                .build();

        String[] stepNames = {
                "Medición inicial",
                "Asignación de turnos",
                "Apertura de surtidores",
                "Registro de lecturas",
                "Cierre de surtidores",
                "Medición final y cuadre"
        };

        for (int i = 0; i < 6; i++) {
            JournalStep step = JournalStep.builder()
                    .journal(journal)
                    .stepNumber(i + 1)
                    .name(stepNames[i])
                    .status(StepStatus.PENDING)
                    .build();
            journal.getSteps().add(step);
        }

        return journalRepository.save(journal);
    }
}
