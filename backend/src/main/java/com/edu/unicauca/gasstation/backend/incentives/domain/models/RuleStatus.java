package com.edu.unicauca.gasstation.backend.incentives.domain.models;

/** State of a rule today: not started, applying, or replaced. */
public enum RuleStatus {
    PROGRAMADO,
    VIGENTE,
    ANTERIOR
}
