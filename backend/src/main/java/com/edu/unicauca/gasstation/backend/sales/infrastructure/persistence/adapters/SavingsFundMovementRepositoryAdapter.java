package com.edu.unicauca.gasstation.backend.sales.infrastructure.persistence.adapters;

import com.edu.unicauca.gasstation.backend.sales.domain.models.SavingsFundMovement;
import com.edu.unicauca.gasstation.backend.sales.domain.models.SavingsFundMovementType;
import com.edu.unicauca.gasstation.backend.sales.domain.repositories.SavingsFundMovementRepository;
import com.edu.unicauca.gasstation.backend.sales.infrastructure.persistence.entities.SavingsFundMovementEntity;
import com.edu.unicauca.gasstation.backend.sales.infrastructure.persistence.repositories.SavingsFundMovementJpaRepository;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class SavingsFundMovementRepositoryAdapter implements SavingsFundMovementRepository {

    private final SavingsFundMovementJpaRepository jpaRepository;

    public SavingsFundMovementRepositoryAdapter(SavingsFundMovementJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public SavingsFundMovement save(SavingsFundMovement movement) {
        return toModel(jpaRepository.save(toEntity(movement)));
    }

    @Override
    public List<SavingsFundMovement> findUpTo(LocalDate date) {
        return jpaRepository.findByMovementDateLessThanEqual(date).stream().map(this::toModel).toList();
    }

    @Override
    public List<SavingsFundMovement> findAll() {
        return jpaRepository.findAll().stream().map(this::toModel).toList();
    }

    @Override
    public List<SavingsFundMovement> findAllNewestFirst() {
        return jpaRepository.findAllByOrderByMovementDateDescIdDesc().stream().map(this::toModel).toList();
    }

    @Override
    public boolean existsContributionOn(LocalDate date) {
        return jpaRepository.existsByMovementTypeAndMovementDate(SavingsFundMovementType.CONTRIBUTION, date);
    }

    private SavingsFundMovementEntity toEntity(SavingsFundMovement m) {
        return new SavingsFundMovementEntity(m.getId(), m.getMovementType(), m.getMovementDate(),
                m.getAmount(), m.getDescription());
    }

    private SavingsFundMovement toModel(SavingsFundMovementEntity e) {
        return new SavingsFundMovement(e.getId(), e.getMovementType(), e.getMovementDate(),
                e.getAmount(), e.getDescription());
    }
}
