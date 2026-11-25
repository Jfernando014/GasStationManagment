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
        String observations) {

    public JournalSnapshot(
            Long id,
            LocalDate date,
            Long consecutive,
            JournalStatus status,
            LocalDateTime createdAt,
            List<JournalStepSnapshot> steps) {
        this(id, date, consecutive, status, createdAt, steps, null);
    }

    public JournalSnapshot {
        steps = List.copyOf(steps);
    }

    public record JournalStepSnapshot(
            Integer stepNumber,
            String name,
            StepStatus status,
            LocalDateTime completedAt) {
    }
}