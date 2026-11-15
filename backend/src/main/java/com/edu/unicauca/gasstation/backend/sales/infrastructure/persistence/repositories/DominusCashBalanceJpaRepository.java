package com.edu.unicauca.gasstation.backend.sales.infrastructure.persistence.repositories;

import com.edu.unicauca.gasstation.backend.sales.infrastructure.persistence.entities.DominusCashBalanceEntity;
import java.time.LocalDate;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DominusCashBalanceJpaRepository extends JpaRepository<DominusCashBalanceEntity, LocalDate> {
}
