package com.edu.unicauca.gasstation.backend.inventory.infrastructure.persistence.repositories;

import com.edu.unicauca.gasstation.backend.inventory.infrastructure.persistence.entities.GaugeTableEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaGaugeTableRepository extends JpaRepository<GaugeTableEntity, Long> {

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, Long id);
}