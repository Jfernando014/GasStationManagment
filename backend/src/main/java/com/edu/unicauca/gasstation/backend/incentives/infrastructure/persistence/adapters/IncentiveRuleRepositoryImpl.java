package com.edu.unicauca.gasstation.backend.incentives.infrastructure.persistence.adapters;

import com.edu.unicauca.gasstation.backend.incentives.domain.models.IncentiveRule;
import com.edu.unicauca.gasstation.backend.incentives.domain.repositories.IncentiveRuleRepository;
import com.edu.unicauca.gasstation.backend.incentives.exception.DuplicateRuleException;
import com.edu.unicauca.gasstation.backend.incentives.infrastructure.persistence.entities.IncentiveRuleEntity;
import com.edu.unicauca.gasstation.backend.incentives.infrastructure.persistence.repositories.JpaIncentiveRuleRepository;
import java.util.Collection;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/** JPA implementation of {@link IncentiveRuleRepository}. */
@Repository
@RequiredArgsConstructor
public class IncentiveRuleRepositoryImpl implements IncentiveRuleRepository {

    private final JpaIncentiveRuleRepository repository;

    @Override
    @Transactional
    public IncentiveRule save(IncentiveRule rule) {
        try {
            // Flushed now so a concurrent duplicate surfaces here as a conflict
            return toDomain(repository.saveAndFlush(toEntity(rule)));
        } catch (DataIntegrityViolationException e) {
            throw new DuplicateRuleException();
        }
    }

    @Override
    public List<IncentiveRule> findHistory(Long productId) {
        return repository.findByProductId(productId).stream().map(this::toDomain).toList();
    }

    @Override
    public List<IncentiveRule> findByProductIds(Collection<Long> productIds) {
        if (productIds.isEmpty()) {
            return List.of();
        }
        return repository.findByProductIdIn(productIds).stream().map(this::toDomain).toList();
    }

    @Override
    public List<IncentiveRule> findAll() {
        return repository.findAll().stream().map(this::toDomain).toList();
    }

    private IncentiveRule toDomain(IncentiveRuleEntity entity) {
        return new IncentiveRule(entity.getId(), entity.getProductId(), entity.getValuePerUnit(),
                entity.getValidFrom(), entity.isActive(), entity.getReason(), entity.getCreatedAt());
    }

    private static IncentiveRuleEntity toEntity(IncentiveRule rule) {
        IncentiveRuleEntity entity = new IncentiveRuleEntity();
        entity.setId(rule.id());
        entity.setProductId(rule.productId());
        entity.setValuePerUnit(rule.valuePerUnit());
        entity.setValidFrom(rule.validFrom());
        entity.setActive(rule.active());
        entity.setReason(rule.reason());
        entity.setCreatedAt(rule.createdAt());
        return entity;
    }
}
