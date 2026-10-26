package com.edu.unicauca.gasstation.backend.incentives.domain.models;

/** A rule of the history with its state today. */
public record RuleHistoryEntry(IncentiveRule rule, RuleStatus status) {
}
