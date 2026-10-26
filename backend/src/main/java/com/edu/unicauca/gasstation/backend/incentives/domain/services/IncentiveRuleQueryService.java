package com.edu.unicauca.gasstation.backend.incentives.domain.services;

import com.edu.unicauca.gasstation.backend.incentives.EffectiveIncentive;
import com.edu.unicauca.gasstation.backend.incentives.IncentiveRuleQuery;
import com.edu.unicauca.gasstation.backend.incentives.domain.models.IncentiveRule;
import com.edu.unicauca.gasstation.backend.incentives.domain.models.RuleTimeline;
import com.edu.unicauca.gasstation.backend.incentives.domain.repositories.IncentiveRuleRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Implements the public {@link IncentiveRuleQuery}. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class IncentiveRuleQueryService implements IncentiveRuleQuery {

    private final IncentiveRuleRepository repository;

    @Override
    public Optional<EffectiveIncentive> findEffective(Long productId, LocalDate date) {
        return RuleTimeline.of(repository.findHistory(productId)).effectiveOn(date)
                .map(IncentiveRuleQueryService::toInfo);
    }

    @Override
    public List<EffectiveIncentive> findAllEffective(LocalDate date) {
        return repository.findAll().stream()
                .collect(Collectors.groupingBy(IncentiveRule::productId, TreeMap::new, Collectors.toList()))
                .values().stream()
                .map(rules -> RuleTimeline.of(rules).effectiveOn(date))
                .flatMap(Optional::stream)
                .map(IncentiveRuleQueryService::toInfo)
                .toList();
    }

    private static EffectiveIncentive toInfo(IncentiveRule rule) {
        return new EffectiveIncentive(rule.productId(), rule.valuePerUnit(), rule.validFrom());
    }
}
