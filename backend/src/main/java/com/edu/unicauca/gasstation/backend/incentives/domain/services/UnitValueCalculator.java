package com.edu.unicauca.gasstation.backend.incentives.domain.services;

import com.edu.unicauca.gasstation.backend.incentives.domain.models.AdditiveSaleUnits;
import com.edu.unicauca.gasstation.backend.incentives.domain.models.RuleTimeline;
import java.math.BigDecimal;
import java.util.Optional;

/**
 * Decides how many pesos one unit of a sale earns. The calculation of a month asks it for every sale, so another
 * way of valuing units can be added by implementing this interface.
 */
public interface UnitValueCalculator {

    /** Pesos per unit for the sale; empty when no value can be given, which leaves the sale unresolved. */
    Optional<BigDecimal> valuePerUnit(RuleTimeline rules, AdditiveSaleUnits sale);
}
