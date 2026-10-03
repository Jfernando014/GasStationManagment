package com.edu.unicauca.gasstation.backend.statistics.domain.services;

import com.edu.unicauca.gasstation.backend.statistics.domain.models.StatisticsModelExample;
import java.util.Optional;

/**
 * Example domain service interface for statistics module.
 */
public interface StatisticsServiceExample {

    Optional<StatisticsModelExample> findById(Long id);

    StatisticsModelExample save(StatisticsModelExample model);
}
