package com.edu.unicauca.gasstation.backend.sales.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record JournalSnapshot(
        Long id,
        LocalDate date,
        Long consecutive,
        JournalStatus status,
        LocalDateTime createdAt,
        List<JournalStepSnapshot> steps,
        String observations,
        List<JournalCellNote> cellNotes) {

    public JournalSnapshot(
            Long id,
            LocalDate date,
            Long consecutive,
            JournalStatus status,
            LocalDateTime createdAt,
            List<JournalStepSnapshot> steps) {
        this(id, date, consecutive, status, createdAt, steps, null, List.of());
    }

    public JournalSnapshot(
            Long id,
            LocalDate date,
            Long consecutive,
            JournalStatus status,
            LocalDateTime createdAt,
            List<JournalStepSnapshot> steps,
            String observations) {
        this(id, date, consecutive, status, createdAt, steps, observations, List.of());
    }

    public JournalSnapshot {
        steps = steps == null ? List.of() : List.copyOf(steps);
        cellNotes = cellNotes == null ? List.of() : List.copyOf(cellNotes);
    }

    public record JournalStepSnapshot(
            Integer stepNumber,
            String name,
            StepStatus status,
            LocalDateTime completedAt,
            String observations) {

        public JournalStepSnapshot(
                Integer stepNumber,
                String name,
                StepStatus status,
                LocalDateTime completedAt) {
            this(stepNumber, name, status, completedAt, null);
        }
    }
}