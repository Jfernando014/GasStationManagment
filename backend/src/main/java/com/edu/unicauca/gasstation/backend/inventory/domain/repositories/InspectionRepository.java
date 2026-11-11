package com.edu.unicauca.gasstation.backend.inventory.domain.repositories;

import com.edu.unicauca.gasstation.backend.inventory.InspectionCategory;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.Inspection;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface InspectionRepository {

    Inspection save(Inspection inspection);

    Optional<Inspection> findById(Long id);

    Optional<Inspection> findByCode(String code);

    boolean existsByCode(String code);

    boolean existsByCodeAndIdNot(String code, Long id);

    List<Inspection> search(InspectionCategory category, LocalDate from, LocalDate to);
}
