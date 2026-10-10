package com.edu.unicauca.gasstation.backend.inventory.infrastructure.persistence.repositories;

import com.edu.unicauca.gasstation.backend.inventory.infrastructure.persistence.entities.TankEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SpringDataTankRepository extends JpaRepository<TankEntity, Long> {

    boolean existsByCode(String code);

    List<TankEntity> findAllByOrderByCodeAsc();
}
