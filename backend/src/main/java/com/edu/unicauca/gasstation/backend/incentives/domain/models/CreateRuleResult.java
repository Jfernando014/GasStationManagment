package com.edu.unicauca.gasstation.backend.incentives.domain.models;

import java.util.List;

/** Outcome of creating a rule; {@code rule} is null when the value was already the one that applies. */
public record CreateRuleResult(boolean created, IncentiveRule rule, String message, List<RuleWarning> warnings) {
}
