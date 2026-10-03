package com.edu.unicauca.gasstation.backend.workers.domain.services;

import com.edu.unicauca.gasstation.backend.workers.domain.models.WorkerModelExample;
import java.util.Optional;

/**
 * Example domain service interface for workers module.
 */
public interface WorkerServiceExample {

    Optional<WorkerModelExample> findById(Long id);

    WorkerModelExample save(WorkerModelExample model);
}
