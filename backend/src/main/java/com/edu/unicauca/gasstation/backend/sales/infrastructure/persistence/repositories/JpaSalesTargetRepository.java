package com.edu.unicauca.gasstation.backend.sales.infrastructure.persistence.repositories;

import com.edu.unicauca.gasstation.backend.sales.domain.models.SalesTargetCategory;
import com.edu.unicauca.gasstation.backend.sales.infrastructure.persistence.entities.SalesTargetEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.repository.Repository;

/** Narrow Spring Data contract: no inherited editing or deletion API. */
public interface JpaSalesTargetRepository extends Repository<SalesTargetEntity, Long> {
    SalesTargetEntity saveAndFlush(SalesTargetEntity entity);
    Optional<SalesTargetEntity> findById(Long id);
    Optional<SalesTargetEntity> findByTargetYearAndTargetMonthAndCategory(
            int year, Short month, SalesTargetCategory category);
    List<SalesTargetEntity> findByTargetYearOrderByTargetMonthAscCategoryAsc(int year);
}
