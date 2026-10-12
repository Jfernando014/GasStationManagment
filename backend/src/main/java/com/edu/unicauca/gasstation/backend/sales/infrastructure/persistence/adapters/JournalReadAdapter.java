package com.edu.unicauca.gasstation.backend.sales.infrastructure.persistence.adapters;

import com.edu.unicauca.gasstation.backend.sales.domain.Journal;
import com.edu.unicauca.gasstation.backend.sales.domain.JournalSnapshot;
import com.edu.unicauca.gasstation.backend.sales.domain.repositories.JournalReadPort;
import com.edu.unicauca.gasstation.backend.sales.domain.JournalRepository;
import java.util.Comparator;
import java.util.Optional;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class JournalReadAdapter implements JournalReadPort {

    private final JournalRepository journalRepository;

    public JournalReadAdapter(JournalRepository journalRepository) {
        this.journalRepository = journalRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<JournalSnapshot> findSnapshotById(Long journalId) {
        return journalRepository.findById(journalId).map(JournalReadAdapter::toSnapshot);
    }

    private static JournalSnapshot toSnapshot(Journal journal) {
        var steps = journal.getSteps().stream()
                .sorted(Comparator.comparing(step -> step.getStepNumber()))
                .map(step -> new JournalSnapshot.JournalStepSnapshot(
                        step.getStepNumber(), step.getName(), step.getStatus(), step.getCompletedAt()))
                .toList();
        return new JournalSnapshot(journal.getId(), journal.getDate(), journal.getConsecutive(),
                journal.getStatus(), journal.getCreatedAt(), steps);
    }
}