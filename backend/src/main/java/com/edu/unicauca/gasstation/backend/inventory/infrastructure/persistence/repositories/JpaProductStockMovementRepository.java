package com.edu.unicauca.gasstation.backend.inventory.infrastructure.persistence.repositories;

import com.edu.unicauca.gasstation.backend.inventory.domain.models.StockLocation;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.StockMovementType;
import com.edu.unicauca.gasstation.backend.inventory.infrastructure.persistence.entities.ProductStockMovementEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface JpaProductStockMovementRepository extends JpaRepository<ProductStockMovementEntity, Long> {

    List<ProductStockMovementEntity> findByJournalDateOrderByIdAsc(LocalDate journalDate);

    boolean existsByProductIdAndLocationAndMovementType(Long productId, StockLocation location,
                                                        StockMovementType movementType);

    @Query("""
            select coalesce(sum(m.quantity), 0L) from ProductStockMovementEntity m
            where m.productId = :productId
              and m.location = :location
              and m.journalDate <= :date
            """)
    Long sumQuantityUpTo(@Param("productId") Long productId, @Param("location") StockLocation location,
                         @Param("date") LocalDate date);

    /** Rows of [productId, location, units]. */
    @Query("""
            select m.productId, m.location, sum(m.quantity) from ProductStockMovementEntity m
            where m.journalDate < :date
            group by m.productId, m.location
            """)
    List<Object[]> findBalancesBefore(@Param("date") LocalDate date);

    @Modifying(clearAutomatically = true)
    @Query("""
            delete from ProductStockMovementEntity m
            where m.journalDate = :date and m.movementType = :movementType
            """)
    void deleteByJournalDateAndMovementType(@Param("date") LocalDate date,
                                            @Param("movementType") StockMovementType movementType);
}