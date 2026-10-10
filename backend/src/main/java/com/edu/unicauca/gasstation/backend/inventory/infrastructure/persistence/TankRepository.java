package com.edu.unicauca.gasstation.backend.inventory.infrastructure.persistence;

import com.edu.unicauca.gasstation.backend.inventory.domain.models.Tank;
import org.springframework.data.jpa.repository.JpaRepository;


public interface TankRepository extends JpaRepository<Tank,Long> {
    boolean existsByCode(String code);

}
