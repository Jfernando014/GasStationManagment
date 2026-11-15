package com.edu.unicauca.gasstation.backend.sales.infrastructure.persistence.adapters;

import com.edu.unicauca.gasstation.backend.sales.domain.models.DominusCashBalance;
import com.edu.unicauca.gasstation.backend.sales.domain.repositories.DominusCashBalanceRepository;
import com.edu.unicauca.gasstation.backend.sales.infrastructure.persistence.entities.DominusCashBalanceEntity;
import com.edu.unicauca.gasstation.backend.sales.infrastructure.persistence.repositories.DominusCashBalanceJpaRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class DominusCashBalanceRepositoryAdapter implements DominusCashBalanceRepository {

    private final DominusCashBalanceJpaRepository jpaRepository;

    public DominusCashBalanceRepositoryAdapter(DominusCashBalanceJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Optional<DominusCashBalance> findByDate(LocalDate date) {
        return jpaRepository.findById(date).map(this::toModel);
    }

    /** The date is the id and is assigned by us, so save() merges: it inserts a new day or replaces an existing one. */
    @Override
    public DominusCashBalance save(DominusCashBalance balance) {
        return toModel(jpaRepository.save(new DominusCashBalanceEntity(
                balance.date(), balance.accumulatedBalance(), Instant.now())));
    }

    private DominusCashBalance toModel(DominusCashBalanceEntity entity) {
        return new DominusCashBalance(entity.getBusinessDate(), entity.getAccumulatedBalance());
    }
}
