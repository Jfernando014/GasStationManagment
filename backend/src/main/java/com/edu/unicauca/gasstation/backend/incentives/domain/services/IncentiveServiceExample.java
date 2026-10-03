package com.edu.unicauca.gasstation.backend.incentives.domain.services;

import com.edu.unicauca.gasstation.backend.incentives.domain.models.IncentiveModelExample;
import java.util.Optional;

/**
 * Example domain service interface for incentives module.
 */
public interface IncentiveServiceExample {

    Optional<IncentiveModelExample> findById(Long id);

    IncentiveModelExample save(IncentiveModelExample model);
}
