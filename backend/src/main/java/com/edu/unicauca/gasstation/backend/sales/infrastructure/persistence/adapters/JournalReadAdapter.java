package com.edu.unicauca.gasstation.backend.sales.infrastructure.persistence.adapters;

import com.edu.unicauca.gasstation.backend.sales.domain.Journal;
import com.edu.unicauca.gasstation.backend.sales.domain.JournalCellNote;
import com.edu.unicauca.gasstation.backend.sales.domain.JournalSnapshot;
import com.edu.unicauca.gasstation.backend.sales.domain.repositories.JournalCellNoteRepository;
import com.edu.unicauca.gasstation.backend.sales.domain.repositories.JournalDateReadPort;
import com.edu.unicauca.gasstation.backend.sales.domain.repositories.JournalReadPort;
import com.edu.unicauca.gasstation.backend.sales.domain.JournalRepository;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class JournalReadAdapter implements JournalReadPort, JournalDateReadPort {

    private final JournalRepository journalRepository;
    private final JournalCellNoteRepository cellNoteRepository;

    public JournalReadAdapter(JournalRepository journalRepository) {
        this(journalRepository, null);
    }

    @Autowired
    public JournalReadAdapter(
            JournalRepository journalRepository,
            @Nullable JournalCellNoteRepository cellNoteRepository) {
        this.journalRepository = journalRepository;
        this.cellNoteRepository = cellNoteRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<JournalSnapshot> findSnapshotById(Long journalId) {
        return journalRepository.findById(journalId).map(this::toSnapshot);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<JournalSnapshot> findSnapshotByDate(LocalDate date) {
        return journalRepository.findByDate(date).map(this::toSnapshot);
    }

    private JournalSnapshot toSnapshot(Journal journal) {
        var steps = journal.getSteps().stream()
                .sorted(Comparator.comparing(step -> step.getStepNumber()))
                .map(step -> new JournalSnapshot.JournalStepSnapshot(
                        step.getStepNumber(),
                        step.getName(),
                        step.getStatus(),
                        step.getCompletedAt(),
                        step.getObservations()))
                .toList();
        List<JournalCellNote> cellNotes = cellNoteRepository == null || journal.getId() == null
                ? List.of()
                : cellNoteRepository.findAllByJournalId(journal.getId());
        return new JournalSnapshot(
                journal.getId(),
                journal.getDate(),
                journal.getConsecutive(),
                journal.getStatus(),
                journal.getCreatedAt(),
                steps,
                journal.getObservations(),
                cellNotes);
    }
}