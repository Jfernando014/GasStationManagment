package com.edu.unicauca.gasstation.backend.inventory.infrastructure.persistence.repositories;

import com.edu.unicauca.gasstation.backend.inventory.infrastructure.persistence.entities.TankReadingEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface JpaTankReadingRepository extends JpaRepository<TankReadingEntity, Long> {

    List<TankReadingEntity> findByMeasurementIdOrderByTankId(Long measurementId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from TankReadingEntity r where r.measurementId = :measurementId")
    void deleteByMeasurementId(@Param("measurementId") Long measurementId);
}