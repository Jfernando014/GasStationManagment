package com.edu.unicauca.gasstation.backend.inventory.infrastructure.persistence.repositories;

import com.edu.unicauca.gasstation.backend.inventory.infrastructure.persistence.entities.GaugePointEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface JpaGaugePointRepository extends JpaRepository<GaugePointEntity, Long> {

    List<GaugePointEntity> findByGaugeTableIdOrderByHeightCm(Long gaugeTableId);

    List<GaugePointEntity> findByGaugeTableIdInOrderByGaugeTableIdAscHeightCmAsc(Collection<Long> gaugeTableIds);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from GaugePointEntity p where p.gaugeTableId = :gaugeTableId")
    void deleteByGaugeTableId(@Param("gaugeTableId") Long gaugeTableId);
}