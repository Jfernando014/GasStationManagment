package com.edu.unicauca.gasstation.backend.inventory.domain.repositories;

import com.edu.unicauca.gasstation.backend.inventory.domain.models.TankMeasurement;

import java.time.LocalDate;
import java.util.Optional;

public interface TankMeasurementRepository {

    TankMeasurement save(TankMeasurement measurement);

    Optional<TankMeasurement> findByJournalDate(LocalDate journalDate);

    boolean existsByJournalDate(LocalDate journalDate);

    Optional<TankMeasurement> findLatestBefore(LocalDate journalDate);

    Optional<TankMeasurement> findLatestContainingTank(Long tankId);
}