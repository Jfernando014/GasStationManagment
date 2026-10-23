package com.edu.unicauca.gasstation.backend.inventory.infrastructure.persistence.repositories;

import com.edu.unicauca.gasstation.backend.inventory.infrastructure.persistence.entities.TankMeasurementEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface JpaTankMeasurementRepository extends JpaRepository<TankMeasurementEntity, Long> {

    Optional<TankMeasurementEntity> findByJournalDate(LocalDate journalDate);

    boolean existsByJournalDate(LocalDate journalDate);

    Optional<TankMeasurementEntity> findFirstByJournalDateLessThanOrderByJournalDateDesc(LocalDate journalDate);
}