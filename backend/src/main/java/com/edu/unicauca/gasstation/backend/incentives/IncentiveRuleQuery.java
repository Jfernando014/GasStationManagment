package com.edu.unicauca.gasstation.backend.incentives;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** Public query of the incentive that applies on a date: the rule with the latest validity not after it. */
public interface IncentiveRuleQuery {

    /** Empty when the additive has no rule valid yet on that date. */
    Optional<EffectiveIncentive> findEffective(Long productId, LocalDate date);

    /** One entry per additive that has a rule valid on that date. */
    List<EffectiveIncentive> findAllEffective(LocalDate date);
}
