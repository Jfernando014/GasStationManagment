package com.edu.unicauca.gasstation.backend.inventory.infrastructure.persistence.repositories;

import com.edu.unicauca.gasstation.backend.inventory.infrastructure.persistence.entities.FuelReceptionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface JpaFuelReceptionRepository extends JpaRepository<FuelReceptionEntity, Long> {

    @Query("""
            select r from FuelReceptionEntity r
            where (:tankId is null or r.tankId = :tankId)
              and (:fromDate is null or r.receptionDate >= :fromDate)
              and (:toDate is null or r.receptionDate <= :toDate)
            order by r.receptionDate desc, r.id desc
            """)
    List<FuelReceptionEntity> search(@Param("tankId") Long tankId,
                                     @Param("fromDate") LocalDate fromDate,
                                     @Param("toDate") LocalDate toDate);

    /** Devuelve null cuando no hay recepciones, el adaptador lo convierte en 0. */
    @Query("""
            select sum(r.gallons) from FuelReceptionEntity r
            where r.tankId = :tankId
              and r.receptionDate > :date
              and (:excludedId is null or r.id <> :excludedId)
            """)
    BigDecimal sumGallonsAfter(@Param("tankId") Long tankId,
                               @Param("date") LocalDate date,
                               @Param("excludedId") Long excludedId);
}