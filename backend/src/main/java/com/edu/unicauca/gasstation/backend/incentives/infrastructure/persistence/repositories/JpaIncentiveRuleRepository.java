package com.edu.unicauca.gasstation.backend.incentives.infrastructure.persistence.repositories;

import com.edu.unicauca.gasstation.backend.incentives.infrastructure.persistence.entities.IncentiveRuleEntity;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data repository used by {@code IncentiveRuleRepositoryImpl}. */
public interface JpaIncentiveRuleRepository extends JpaRepository<IncentiveRuleEntity, Long> {

    List<IncentiveRuleEntity> findByProductId(Long productId);

    List<IncentiveRuleEntity> findByProductIdIn(Collection<Long> productIds);
}
