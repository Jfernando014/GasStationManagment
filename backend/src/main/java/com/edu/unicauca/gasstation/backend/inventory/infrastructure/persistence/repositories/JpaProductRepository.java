package com.edu.unicauca.gasstation.backend.inventory.infrastructure.persistence.repositories;

import com.edu.unicauca.gasstation.backend.inventory.ProductCategory;
import com.edu.unicauca.gasstation.backend.inventory.infrastructure.persistence.entities.ProductEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface JpaProductRepository extends JpaRepository<ProductEntity, Long> {

    @Query("""
            select p from ProductEntity p
            where (:category is null or p.category = :category)
              and (:active is null or p.active = :active)
            order by p.name
            """)
    List<ProductEntity> search(@Param("category") ProductCategory category,
                               @Param("active") Boolean active);

    Boolean existsByCode(String code);

    Boolean existsByCodeAndIdNot(String code, Long id);
}