package com.edu.unicauca.gasstation.backend.workers.domain.repositories;

import com.edu.unicauca.gasstation.backend.workers.domain.models.Worker;
import com.edu.unicauca.gasstation.backend.workers.domain.models.WorkerFilter;
import com.edu.unicauca.gasstation.backend.workers.exception.DuplicateDocumentException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Port for storing and reading workers. Implemented by
 * {@code infrastructure.persistence.adapters.WorkerRepositoryImpl}.
 */
public interface WorkerRepository {

    /**
     * Creates or updates a worker and writes it immediately.
     *
     * @throws DuplicateDocumentException if the database rejects the document as duplicated
     *                                    (two concurrent requests passed the check of the service)
     */
    Worker save(Worker worker);

    Optional<Worker> findById(UUID id);

    /** Used when creating: is the document already taken by any worker? */
    boolean existsByDocument(String document);

    /** Used when editing: is the document taken by a worker other than the one being edited? */
    boolean existsByDocumentAndIdNot(String document, UUID id);

    /** Workers matching every filter that is set, ordered by full name. */
    List<Worker> findAll(WorkerFilter filter);
}
