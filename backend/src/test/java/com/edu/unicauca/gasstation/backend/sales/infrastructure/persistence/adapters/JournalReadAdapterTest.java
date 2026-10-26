package com.edu.unicauca.gasstation.backend.sales.infrastructure.persistence.adapters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.edu.unicauca.gasstation.backend.sales.domain.Journal;
import com.edu.unicauca.gasstation.backend.sales.domain.JournalRepository;
import com.edu.unicauca.gasstation.backend.sales.domain.JournalSnapshot;
import com.edu.unicauca.gasstation.backend.sales.domain.JournalStatus;
import com.edu.unicauca.gasstation.backend.sales.domain.JournalStep;
import com.edu.unicauca.gasstation.backend.sales.domain.StepStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class JournalReadAdapterTest {

    private static final LocalDate DAY = LocalDate.of(2026, 8, 5);

    @Mock
    private JournalRepository journalRepository;

    @InjectMocks
    private JournalReadAdapter adapter;

    private static JournalStep step(int number, String name) {
        return JournalStep.builder().stepNumber(number).name(name).status(StepStatus.PENDING).build();
    }

    @Test
    void findsTheJournalByDateAndMapsItToASnapshotWithOrderedSteps() {
        Journal journal = Journal.builder()
                .id(5L)
                .date(DAY)
                .consecutive(288L)
                .status(JournalStatus.OPEN)
                .createdAt(LocalDateTime.of(2026, 8, 5, 7, 0))
                .steps(new ArrayList<>(List.of(step(2, "Second"), step(1, "First"))))
                .build();
        when(journalRepository.findByDate(DAY)).thenReturn(Optional.of(journal));

        JournalSnapshot snapshot = adapter.findSnapshotByDate(DAY).orElseThrow();

        assertEquals(5L, snapshot.id());
        assertEquals(DAY, snapshot.date());
        assertEquals(288L, snapshot.consecutive());
        assertEquals(JournalStatus.OPEN, snapshot.status());
        assertEquals(List.of(1, 2), snapshot.steps().stream().map(JournalSnapshot.JournalStepSnapshot::stepNumber).toList());
    }

    @Test
    void returnsEmptyWhenThereIsNoJournalOnTheDate() {
        when(journalRepository.findByDate(DAY)).thenReturn(Optional.empty());

        assertTrue(adapter.findSnapshotByDate(DAY).isEmpty());
    }
}
