package com.edu.unicauca.gasstation.backend.sales.infrastructure.persistence.adapters;

import com.edu.unicauca.gasstation.backend.inventory.JournalStepPort;
import com.edu.unicauca.gasstation.backend.sales.domain.JournalRepository;
import com.edu.unicauca.gasstation.backend.sales.domain.services.JournalStepReset;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Lets Inventory ask whether a journal is closed and return one of its steps to PENDING. */
@Component
@RequiredArgsConstructor
public class JournalStepAdapter implements JournalStepPort {

    private final JournalRepository journals;

    @Override
    @Transactional(readOnly = true)
    public boolean isJournalClosed(LocalDate journalDate) {
        return journals.findByDate(journalDate).map(JournalStepReset::isClosed).orElse(false);
    }

    @Override
    @Transactional
    public void returnStepToPending(LocalDate journalDate, int stepNumber) {
        journals.findByDate(journalDate).ifPresent(journal -> {
            if (JournalStepReset.returnToPending(journal, stepNumber)) {
                journals.save(journal);
            }
        });
    }
}
