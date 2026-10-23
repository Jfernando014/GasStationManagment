package com.edu.unicauca.gasstation.backend.competition.infrastructure.persistence.repositories;

import com.edu.unicauca.gasstation.backend.competition.infrastructure.persistence.entities.CompetitorEntity;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface JpaCompetitorRepository extends Repository<CompetitorEntity, Long> {
    CompetitorEntity saveAndFlush(CompetitorEntity entity);
    Optional<CompetitorEntity> findById(Long id);
    @Query("select c from CompetitorEntity c where (:active is null or c.active = :active) order by c.name, c.id")
    List<CompetitorEntity> findFiltered(@Param("active") Boolean active);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from CompetitorEntity c where c.id = :id")
    Optional<CompetitorEntity> findLocked(@Param("id") long id);
}
