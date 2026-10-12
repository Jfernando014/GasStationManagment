package com.edu.unicauca.gasstation.backend.sales.infrastructure.persistence.repositories;

import com.edu.unicauca.gasstation.backend.sales.domain.models.SavingsFundMovementType;
import com.edu.unicauca.gasstation.backend.sales.infrastructure.persistence.entities.SavingsFundMovementEntity;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SavingsFundMovementJpaRepository extends JpaRepository<SavingsFundMovementEntity, Long> {

    List<SavingsFundMovementEntity> findByMovementDateLessThanEqual(LocalDate date);

    List<SavingsFundMovementEntity> findAllByOrderByMovementDateDescIdDesc();

    boolean existsByMovementTypeAndMovementDate(SavingsFundMovementType type, LocalDate date);
}
