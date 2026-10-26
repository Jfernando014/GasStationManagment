package com.edu.unicauca.gasstation.backend.incentives.domain.repositories;

import com.edu.unicauca.gasstation.backend.incentives.domain.models.IncentiveRule;
import java.util.Collection;
import java.util.List;

/** Storage of incentive rules. Rules are only added. */
public interface IncentiveRuleRepository {

    /** Throws {@code DuplicateRuleException} if the additive already has a rule with that validity. */
    IncentiveRule save(IncentiveRule rule);

    /** Every rule of the additive. */
    List<IncentiveRule> findHistory(Long productId);

    /** Every rule of those additives. */
    List<IncentiveRule> findByProductIds(Collection<Long> productIds);

    /** Every rule of every additive. */
    List<IncentiveRule> findAll();
}
