package com.edu.unicauca.gasstation.backend.competition.infrastructure.persistence.repositories;

import com.edu.unicauca.gasstation.backend.competition.infrastructure.persistence.entities.ObservationEntity;
import com.edu.unicauca.gasstation.backend.inventory.FuelType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface JpaObservationRepository extends Repository<ObservationEntity, Long> {
    ObservationEntity saveAndFlush(ObservationEntity entity);
    Optional<ObservationEntity> findById(Long id);

    @Query("""
            select o from ObservationEntity o
            where (:competitorId is null or o.competitorId = :competitorId)
              and (:fuelType is null or o.fuelType = :fuelType)
              and o.observedAt >= :from and o.observedAt <= :to
            order by o.observedAt desc, o.id desc
            """)
    Page<ObservationEntity> findHistory(@Param("competitorId") Long competitorId,
            @Param("fuelType") FuelType fuelType, @Param("from") Instant from,
            @Param("to") Instant to, Pageable pageable);

    @Query(value = """
            SELECT DISTINCT ON (o.competitor_id, o.fuel_type) o.*
            FROM competitor_price_observation o JOIN competitor c ON c.id = o.competitor_id
            WHERE (CAST(:competitorId AS bigint) IS NULL OR o.competitor_id = :competitorId)
              AND (CAST(:fuelType AS varchar) IS NULL OR o.fuel_type = :fuelType)
              AND o.observed_at <= :asOf AND (:includeInactive = true OR c.active = true)
            ORDER BY o.competitor_id, o.fuel_type, o.observed_at DESC, o.id DESC
            """, nativeQuery = true)
    List<ObservationEntity> findLatest(@Param("competitorId") Long competitorId,
            @Param("fuelType") String fuelType, @Param("asOf") Instant asOf,
            @Param("includeInactive") boolean includeInactive);
}
