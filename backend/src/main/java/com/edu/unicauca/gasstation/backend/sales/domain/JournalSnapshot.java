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
        List<JournalStepSnapshot> steps) {

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