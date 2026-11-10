package com.edu.unicauca.gasstation.backend.inventory.infrastructure.persistence.repositories;

import com.edu.unicauca.gasstation.backend.inventory.InspectionCategory;
import com.edu.unicauca.gasstation.backend.inventory.infrastructure.persistence.entities.InspectionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface JpaInspectionRepository extends JpaRepository<InspectionEntity, Long> {

    @Query("""
            select i from InspectionEntity i
            where (:category is null or i.category = :category)
              and i.dateExecuted between :fromDate and :toDate
            order by i.dateDue asc, i.id desc
            """)
    List<InspectionEntity> search(@Param("category") InspectionCategory category,
                                  @Param("fromDate") LocalDate fromDate,
                                  @Param("toDate") LocalDate toDate);

    Optional<InspectionEntity> findByCode(String code);

    boolean existsByCode(String code);

    boolean existsByCodeAndIdNot(String code, Long id);
}
