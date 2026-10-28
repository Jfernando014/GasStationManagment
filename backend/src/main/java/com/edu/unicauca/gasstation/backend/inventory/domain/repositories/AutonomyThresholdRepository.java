package com.edu.unicauca.gasstation.backend.inventory.domain.repositories;

import com.edu.unicauca.gasstation.backend.inventory.domain.models.AutonomyThreshold;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AutonomyThresholdRepository {

    AutonomyThreshold save(AutonomyThreshold threshold);

    /** The threshold with the latest validity date that is not after the given date. */
    Optional<AutonomyThreshold> findEffectiveOn(LocalDate date);

    boolean existsByValidFrom(LocalDate validFrom);

    /** All thresholds, most recent validity first. */
    List<AutonomyThreshold> findAll();
}