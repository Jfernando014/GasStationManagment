package com.edu.unicauca.gasstation.backend.incentives.domain.models;

/** Notice that goes with a created rule without blocking it. */
public record RuleWarning(Code code, String message) {

    public enum Code {
        UNUSUALLY_HIGH_VALUE,
        VALID_FROM_NOT_FIRST_DAY
    }
}
