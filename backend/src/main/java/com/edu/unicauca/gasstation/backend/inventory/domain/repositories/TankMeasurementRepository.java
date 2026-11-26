package com.edu.unicauca.gasstation.backend.inventory.domain.repositories;

import com.edu.unicauca.gasstation.backend.inventory.domain.models.TankMeasurement;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TankMeasurementRepository {

    TankMeasurement save(TankMeasurement measurement);

    Optional<TankMeasurement> findByJournalDate(LocalDate journalDate);

    /** Measurements of the dates between both (inclusive) with their readings, read at once and ordered by date. */
    List<TankMeasurement> findByJournalDateBetween(LocalDate from, LocalDate to);

    boolean existsByJournalDate(LocalDate journalDate);

    /** Removes the measurement of the date with its readings, if any. */
    void deleteByJournalDate(LocalDate journalDate);

    Optional<TankMeasurement> findLatestBefore(LocalDate journalDate);

    Optional<TankMeasurement> findLatestContainingTank(Long tankId);
}