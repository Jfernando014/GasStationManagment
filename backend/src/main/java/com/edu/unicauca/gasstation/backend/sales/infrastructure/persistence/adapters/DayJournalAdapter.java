package com.edu.unicauca.gasstation.backend.sales.infrastructure.persistence.adapters;

import com.edu.unicauca.gasstation.backend.sales.domain.Journal;
import com.edu.unicauca.gasstation.backend.sales.domain.JournalRepository;
import com.edu.unicauca.gasstation.backend.sales.domain.JournalStatus;
import com.edu.unicauca.gasstation.backend.sales.domain.JournalStep;
import com.edu.unicauca.gasstation.backend.sales.domain.StepStatus;
import com.edu.unicauca.gasstation.backend.sales.domain.services.JournalService;
import com.edu.unicauca.gasstation.backend.shifts.daystaff.DayStaff.Step1Status;
import com.edu.unicauca.gasstation.backend.shifts.spi.DayJournalPort;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Real adapter implementing {@link DayJournalPort} from the {@code shifts} module.
 * Satisfies the port declared by Turnos without creating circular dependencies:
 * {@code sales} depends on {@code shifts}, while {@code shifts} only depends on its own port.
 * When this bean is present, {@code TemporaryDayJournalPort} is automatically backed off.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DayJournalAdapter implements DayJournalPort {

    private final JournalRepository journalRepository;
    private final JournalService journalService;

    @Override
    @Transactional(readOnly = true)
    public Step1Status step1Status(LocalDate date) {
        if (date == null) {
            return Step1Status.NO_JOURNAL;
        }
        Optional<Journal> journalOpt = journalRepository.findByDate(date);
        if (journalOpt.isEmpty()) {
            return Step1Status.NO_JOURNAL;
        }

        Journal journal = journalOpt.get();
        if (journal.getSteps() == null) {
            return Step1Status.PENDING;
        }

        return journal.getSteps().stream()
                .filter(step -> step.getStepNumber() != null && step.getStepNumber() == 1)
                .findFirst()
                .map(step -> step.getStatus() == StepStatus.COMPLETED
                        ? Step1Status.CONFIRMED
                        : Step1Status.PENDING)
                .orElse(Step1Status.PENDING);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isEditable(LocalDate date) {
        if (date == null) {
            return false;
        }
        Optional<Journal> journalOpt = journalRepository.findByDate(date);
        // If no journal exists yet, it can be created/edited; if it exists, it must be OPEN
        return journalOpt.map(j -> j.getStatus() == JournalStatus.OPEN).orElse(true);
    }

    @Override
    @Transactional
    public void markStep1Completed(LocalDate date) {
        Objects.requireNonNull(date, "date");
        if (!isEditable(date)) {
            throw new IllegalStateException("The journal of " + date + " is closed");
        }

        Journal journal = journalService.getOrCreateJournal(date);
        JournalStep step1 = journal.getSteps().stream()
                .filter(step -> step.getStepNumber() != null && step.getStepNumber() == 1)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Step 1 not found for journal " + date));

        step1.setStatus(StepStatus.COMPLETED);
        step1.setCompletedAt(LocalDateTime.now());

        // Advance Step 2 to IN_PROGRESS if it was PENDING
        journal.getSteps().stream()
                .filter(step -> step.getStepNumber() != null && step.getStepNumber() == 2)
                .findFirst()
                .ifPresent(step2 -> {
                    if (step2.getStatus() == StepStatus.PENDING) {
                        step2.setStatus(StepStatus.IN_PROGRESS);
                    }
                });

        journalRepository.save(journal);
        log.info("Marked step 1 as COMPLETED for journal date {}", date);
    }
}
