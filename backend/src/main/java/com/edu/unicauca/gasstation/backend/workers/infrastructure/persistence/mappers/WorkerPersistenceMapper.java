package com.edu.unicauca.gasstation.backend.workers.infrastructure.persistence.mappers;

import com.edu.unicauca.gasstation.backend.workers.domain.models.Worker;
import com.edu.unicauca.gasstation.backend.workers.infrastructure.persistence.entities.WorkerEntity;
import org.springframework.stereotype.Component;

/**
 * Converts between the domain {@link Worker} and the JPA {@link WorkerEntity}.
 */
@Component
public class WorkerPersistenceMapper {

    public Worker toDomain(WorkerEntity entity) {
        return Worker.restore(entity.getId(), entity.getFullName(), entity.getDocument(),
                entity.getRoleId(), entity.isActive());
    }

    public WorkerEntity toEntity(Worker worker) {
        WorkerEntity entity = new WorkerEntity();
        entity.setId(worker.getId());
        entity.setFullName(worker.getFullName());
        entity.setDocument(worker.getDocument());
        entity.setRoleId(worker.getRoleId());
        entity.setActive(worker.isActive());
        return entity;
    }
}
