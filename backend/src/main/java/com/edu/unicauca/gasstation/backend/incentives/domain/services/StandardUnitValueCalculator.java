package com.edu.unicauca.gasstation.backend.incentives.domain.services;

import com.edu.unicauca.gasstation.backend.incentives.domain.models.AdditiveSaleUnits;
import com.edu.unicauca.gasstation.backend.incentives.domain.models.RuleTimeline;
import java.math.BigDecimal;
import java.util.Optional;
import org.springframework.stereotype.Component;

/** Standard mode: every unit earns the value of the rule in force on the day it was sold. */
@Component
public class StandardUnitValueCalculator implements UnitValueCalculator {

    @Override
    public Optional<BigDecimal> valuePerUnit(RuleTimeline rules, AdditiveSaleUnits sale) {
        return rules.effectiveOn(sale.journalDate()).map(rule -> BigDecimal.valueOf(rule.valuePerUnit()));
    }
}
