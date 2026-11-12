package com.edu.unicauca.gasstation.backend.workers.domain.models;

/** The fixed overtime hours of a fortnight; {@code hours} is 0 and {@code applies} false when the role has it off. */
public record FixedHedoLine(boolean applies, int hours) {
}
