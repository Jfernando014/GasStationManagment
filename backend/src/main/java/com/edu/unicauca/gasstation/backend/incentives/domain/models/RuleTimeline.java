package com.edu.unicauca.gasstation.backend.incentives.domain.models;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Rules of one additive ordered by validity. The only place that decides which rule applies on a date: the latest
 * one that started on or before it.
 */
public final class RuleTimeline {

    private final List<IncentiveRule> rules;

    private RuleTimeline(List<IncentiveRule> rules) {
        this.rules = rules;
    }

    public static RuleTimeline of(Collection<IncentiveRule> rules) {
        return new RuleTimeline(rules.stream().sorted(Comparator.comparing(IncentiveRule::validFrom)).toList());
    }

    public boolean isEmpty() {
        return rules.isEmpty();
    }

    /** Rule that applies on {@code date}; empty if the first rule starts later. */
    public Optional<IncentiveRule> effectiveOn(LocalDate date) {
        return rules.stream().filter(rule -> !rule.validFrom().isAfter(date)).reduce((older, newer) -> newer);
    }

    /** Nearest rule that starts after {@code date}. */
    public Optional<IncentiveRule> nextAfter(LocalDate date) {
        return rules.stream().filter(rule -> rule.validFrom().isAfter(date)).findFirst();
    }

    /** Rule that starts exactly on {@code date}. */
    public Optional<IncentiveRule> startingOn(LocalDate date) {
        return rules.stream().filter(rule -> rule.validFrom().equals(date)).findFirst();
    }

    /** Every rule, newest first, with its state on {@code today}. */
    public List<RuleHistoryEntry> historyOn(LocalDate today) {
        IncentiveRule current = effectiveOn(today).orElse(null);
        return rules.reversed().stream()
                .map(rule -> new RuleHistoryEntry(rule, statusOf(rule, today, current)))
                .toList();
    }

    private static RuleStatus statusOf(IncentiveRule rule, LocalDate today, IncentiveRule current) {
        if (rule.validFrom().isAfter(today)) {
            return RuleStatus.PROGRAMADO;
        }
        return rule == current ? RuleStatus.VIGENTE : RuleStatus.ANTERIOR;
    }
}
