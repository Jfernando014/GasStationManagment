package com.edu.unicauca.gasstation.backend.sales.infrastructure.persistence;

import com.edu.unicauca.gasstation.backend.sales.domain.models.CashMovement;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CashMovementRepository extends JpaRepository<CashMovement, Long> {

    List<CashMovement> findByMovementDateLessThanEqual(LocalDate date);

    List<CashMovement> findAllByOrderByMovementDateDescIdDesc();
}
