package com.edu.unicauca.gasstation.backend.inventory.domain.repositories;

import com.edu.unicauca.gasstation.backend.inventory.domain.models.Tank;

import java.util.List;
import java.util.Optional;

public interface TankRepository {

    Tank save(Tank tank);

    Optional<Tank> findById(Long id);

    List<Tank> findAllOrderByCode();

    boolean existsByCode(String code);
}
