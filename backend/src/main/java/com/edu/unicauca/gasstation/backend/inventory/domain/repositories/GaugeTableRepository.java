package com.edu.unicauca.gasstation.backend.inventory.domain.repositories;

import com.edu.unicauca.gasstation.backend.inventory.domain.models.GaugeTable;

import java.util.List;
import java.util.Optional;

public interface GaugeTableRepository {

    GaugeTable save(GaugeTable table);

    Optional<GaugeTable> findById(Long id);

    List<GaugeTable> findAllOrderByName();

    boolean existsById(Long id);

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, Long id);
}