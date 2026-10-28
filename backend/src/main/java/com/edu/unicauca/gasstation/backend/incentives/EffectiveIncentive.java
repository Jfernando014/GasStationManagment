package com.edu.unicauca.gasstation.backend.incentives;

import java.time.LocalDate;

/** Pesos per unit that apply to an additive, and the day that value started. */
public record EffectiveIncentive(Long productId, int valuePerUnit, LocalDate validFrom) {
}
