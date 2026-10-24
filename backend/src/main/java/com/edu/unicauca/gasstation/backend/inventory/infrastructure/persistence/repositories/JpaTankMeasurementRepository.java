package com.edu.unicauca.gasstation.backend.inventory.infrastructure.persistence.repositories;

import com.edu.unicauca.gasstation.backend.inventory.infrastructure.persistence.entities.TankMeasurementEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface JpaTankMeasurementRepository extends JpaRepository<TankMeasurementEntity, Long> {

    Optional<TankMeasurementEntity> findByJournalDate(LocalDate journalDate);

    boolean existsByJournalDate(LocalDate journalDate);

    Optional<TankMeasurementEntity> findFirstByJournalDateLessThanOrderByJournalDateDesc(LocalDate journalDate);

    @Query("""
            select m from TankMeasurementEntity m
            where exists (select 1 from TankReadingEntity r
                          where r.measurementId = m.id and r.tankId = :tankId)
            order by m.journalDate desc
            """)
    List<TankMeasurementEntity> findContainingTank(@Param("tankId") Long tankId, Pageable pageable);
}