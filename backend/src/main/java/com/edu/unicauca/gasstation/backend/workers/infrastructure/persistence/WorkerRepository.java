package com.edu.unicauca.gasstation.backend.workers.infrastructure.persistence;

import com.edu.unicauca.gasstation.backend.workers.domain.models.Worker;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/**
 * Spring Data repository of {@link Worker}. {@link JpaSpecificationExecutor} builds the list with optional
 * filters ({@link WorkerSpecifications}) without one query per filter combination.
 */
public interface WorkerRepository extends JpaRepository<Worker, UUID>, JpaSpecificationExecutor<Worker> {

    /** Used when creating: is the document already taken by any worker? */
    boolean existsByDocument(String document);

    /** Used when editing: is the document taken by a worker other than the one being edited? */
    boolean existsByDocumentAndIdNot(String document, UUID id);
}
