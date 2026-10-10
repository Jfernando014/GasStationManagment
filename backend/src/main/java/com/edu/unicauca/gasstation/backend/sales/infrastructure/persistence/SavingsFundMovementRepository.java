package com.edu.unicauca.gasstation.backend.sales.infrastructure.persistence;

import com.edu.unicauca.gasstation.backend.sales.domain.models.SavingsFundMovement;
import com.edu.unicauca.gasstation.backend.sales.domain.models.SavingsFundMovementType;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SavingsFundMovementRepository extends JpaRepository<SavingsFundMovement, Long> {

    List<SavingsFundMovement> findByMovementDateLessThanEqual(LocalDate date);

    boolean existsByMovementTypeAndMovementDate(SavingsFundMovementType type, LocalDate date);

    List<SavingsFundMovement> findAllByOrderByMovementDateDescIdDesc();
}
