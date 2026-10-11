package com.edu.unicauca.gasstation.backend.workers.infrastructure.persistence.repositories;

import com.edu.unicauca.gasstation.backend.workers.infrastructure.persistence.entities.WorkerEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/**
 * Spring Data repository of {@link WorkerEntity}. Used only by {@code WorkerRepositoryImpl}.
 * {@link JpaSpecificationExecutor} builds the list with optional filters without one query per combination.
 */
public interface JpaWorkerRepository extends JpaRepository<WorkerEntity, UUID>, JpaSpecificationExecutor<WorkerEntity> {

    boolean existsByDocument(String document);

    boolean existsByDocumentAndIdNot(String document, UUID id);
}
