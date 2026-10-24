package com.edu.unicauca.gasstation.backend.sales.infrastructure.persistence.repositories;

import com.edu.unicauca.gasstation.backend.sales.infrastructure.persistence.entities.SalesDiscrepancyToleranceEntity;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SalesDiscrepancyToleranceJpaRepository extends JpaRepository<SalesDiscrepancyToleranceEntity, Long> {

    /**
     * Resolves the active tolerance version for a given date D:
     * finds the version with the greatest effective_date <= D.
     */
    Optional<SalesDiscrepancyToleranceEntity> findFirstByEffectiveDateLessThanEqualOrderByEffectiveDateDesc(LocalDate date);

    Optional<SalesDiscrepancyToleranceEntity> findByEffectiveDate(LocalDate effectiveDate);

    List<SalesDiscrepancyToleranceEntity> findAllByOrderByEffectiveDateDesc();

    List<SalesDiscrepancyToleranceEntity> findAllByEffectiveDateGreaterThanOrderByEffectiveDateAsc(LocalDate date);
}
