package com.edu.unicauca.gasstation.backend.sales.infrastructure.persistence.repositories;

import com.edu.unicauca.gasstation.backend.sales.infrastructure.persistence.entities.CashMovementEntity;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CashMovementJpaRepository extends JpaRepository<CashMovementEntity, Long> {

    List<CashMovementEntity> findByMovementDateLessThanEqual(LocalDate date);

    List<CashMovementEntity> findAllByOrderByMovementDateDescIdDesc();
}
