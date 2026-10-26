package com.edu.unicauca.gasstation.backend.inventory.infrastructure.persistence.repositories;

import com.edu.unicauca.gasstation.backend.inventory.infrastructure.persistence.entities.AutonomyThresholdEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface JpaAutonomyThresholdRepository extends JpaRepository<AutonomyThresholdEntity, Long> {

    Optional<AutonomyThresholdEntity> findFirstByValidFromLessThanEqualOrderByValidFromDesc(LocalDate date);

    boolean existsByValidFrom(LocalDate validFrom);

    List<AutonomyThresholdEntity> findAllByOrderByValidFromDesc();
}