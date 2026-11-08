package com.edu.unicauca.gasstation.backend.sales.domain.services;

import com.edu.unicauca.gasstation.backend.sales.JournalClosedEvent;
import com.edu.unicauca.gasstation.backend.sales.domain.*;
import com.edu.unicauca.gasstation.backend.sales.exception.JournalNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Optional;

@Service
public class JournalService {

    private final JournalRepository journalRepository;
    private final ApplicationEventPublisher eventPublisher;

    public JournalService(JournalRepository journalRepository) {
        this(journalRepository, null);
    }

    @Autowired
    public JournalService(JournalRepository journalRepository, ApplicationEventPublisher eventPublisher) {
        this.journalRepository = journalRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public Journal getOrCreateJournal(LocalDate date) {
        return journalRepository.findByDate(date).orElseGet(() -> createJournal(date));
    }

    @Transactional(readOnly = true)
    public Journal getJournalById(Long journalId) {
        return journalRepository.findById(journalId)
                .orElseThrow(() -> new JournalNotFoundException(journalId));
    }

    @Transactional(readOnly = true)
    public Optional<Journal> findJournalByDate(LocalDate date) {
        return journalRepository.findByDate(date);
    }

    /**
     * Closes the operational journal (Step 6, C-08), locking all steps in read-only mode
     * and publishing {@link JournalClosedEvent} (S3-03, S3-04).
     */
    @Transactional
    public Journal closeJournal(Long journalId) {
        Journal journal = journalRepository.findById(journalId)
                .orElseThrow(() -> new JournalNotFoundException(journalId));

        if (journal.getStatus() == JournalStatus.CLOSED) {
            throw new IllegalStateException(
                    "La jornada del " + journal.getDate() + " ya está cerrada y no permite modificaciones.");
        }

        LocalDateTime closedAt = LocalDateTime.now();
        if (journal.getSteps() != null) {
            journal.getSteps().stream()
                    .filter(step -> step.getStepNumber() != null && step.getStepNumber() == 6)
                    .findFirst()
                    .ifPresent(step6 -> {
                        step6.setStatus(StepStatus.COMPLETED);
                        step6.setCompletedAt(closedAt);
                    });
        }

        journal.setStatus(JournalStatus.CLOSED);
        Journal saved = journalRepository.save(journal);

        if (eventPublisher != null) {
            eventPublisher.publishEvent(new JournalClosedEvent(
                    saved.getId(),
                    saved.getDate(),
                    saved.getConsecutive(),
                    closedAt
            ));
        }

        return saved;
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
