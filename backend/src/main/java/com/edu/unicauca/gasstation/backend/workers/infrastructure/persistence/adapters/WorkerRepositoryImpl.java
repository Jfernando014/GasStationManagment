package com.edu.unicauca.gasstation.backend.workers.infrastructure.persistence.adapters;

import com.edu.unicauca.gasstation.backend.workers.domain.models.Worker;
import com.edu.unicauca.gasstation.backend.workers.domain.models.WorkerFilter;
import com.edu.unicauca.gasstation.backend.workers.domain.repositories.WorkerRepository;
import com.edu.unicauca.gasstation.backend.workers.exception.DuplicateDocumentException;
import com.edu.unicauca.gasstation.backend.workers.infrastructure.persistence.WorkerSpecifications;
import com.edu.unicauca.gasstation.backend.workers.infrastructure.persistence.entities.WorkerEntity;
import com.edu.unicauca.gasstation.backend.workers.infrastructure.persistence.mappers.WorkerPersistenceMapper;
import com.edu.unicauca.gasstation.backend.workers.infrastructure.persistence.repositories.JpaWorkerRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

/**
 * Implements the domain {@link WorkerRepository} with Spring Data JPA.
 */
@Repository
@RequiredArgsConstructor
public class WorkerRepositoryImpl implements WorkerRepository {

    private final JpaWorkerRepository repository;
    private final WorkerPersistenceMapper mapper;

    /**
     * Uses {@code saveAndFlush} so the INSERT/UPDATE is sent now: if two concurrent requests passed the document
     * check, the database UNIQUE constraint rejects the second one here and it becomes a domain exception.
     */
    @Override
    public Worker save(Worker worker) {
        try {
            return mapper.toDomain(repository.saveAndFlush(mapper.toEntity(worker)));
        } catch (DataIntegrityViolationException ex) {
            String cause = ex.getMostSpecificCause().getMessage();
            if (cause != null && cause.contains(WorkerEntity.UNIQUE_DOCUMENT_CONSTRAINT)) {
                throw new DuplicateDocumentException(worker.getDocument());
            }
            throw ex;
        }
    }

    @Override
    public Optional<Worker> findById(UUID id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public boolean existsByDocument(String document) {
        return repository.existsByDocument(document);
    }

    @Override
    public boolean existsByDocumentAndIdNot(String document, UUID id) {
        return repository.existsByDocumentAndIdNot(document, id);
    }

    @Override
    public List<Worker> findAll(WorkerFilter filter) {
        return repository.findAll(WorkerSpecifications.matching(filter), Sort.by("fullName")).stream()
                .map(mapper::toDomain)
                .toList();
    }
}
