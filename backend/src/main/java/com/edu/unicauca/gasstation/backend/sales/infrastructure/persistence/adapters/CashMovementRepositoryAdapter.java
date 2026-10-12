package com.edu.unicauca.gasstation.backend.sales.infrastructure.persistence.adapters;

import com.edu.unicauca.gasstation.backend.sales.domain.models.CashMovement;
import com.edu.unicauca.gasstation.backend.sales.domain.repositories.CashMovementRepository;
import com.edu.unicauca.gasstation.backend.sales.infrastructure.persistence.entities.CashMovementEntity;
import com.edu.unicauca.gasstation.backend.sales.infrastructure.persistence.repositories.CashMovementJpaRepository;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class CashMovementRepositoryAdapter implements CashMovementRepository {

    private final CashMovementJpaRepository jpaRepository;

    public CashMovementRepositoryAdapter(CashMovementJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public CashMovement save(CashMovement movement) {
        return toModel(jpaRepository.save(toEntity(movement)));
    }

    @Override
    public List<CashMovement> findUpTo(LocalDate date) {
        return jpaRepository.findByMovementDateLessThanEqual(date).stream().map(this::toModel).toList();
    }

    @Override
    public List<CashMovement> findAllNewestFirst() {
        return jpaRepository.findAllByOrderByMovementDateDescIdDesc().stream().map(this::toModel).toList();
    }

    private CashMovementEntity toEntity(CashMovement m) {
        return new CashMovementEntity(m.getId(), m.getMovementType(), m.getMovementDate(),
                m.getAmount(), m.getDescription());
    }

    private CashMovement toModel(CashMovementEntity e) {
        return new CashMovement(e.getId(), e.getMovementType(), e.getMovementDate(),
                e.getAmount(), e.getDescription());
    }
}
