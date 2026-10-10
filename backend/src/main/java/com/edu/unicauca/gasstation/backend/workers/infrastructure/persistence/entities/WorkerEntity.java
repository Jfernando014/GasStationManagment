package com.edu.unicauca.gasstation.backend.workers.infrastructure.persistence.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

/**
 * JPA mapping of the {@code worker} table (migration V7). Only used inside the persistence layer.
 *
 * <p>{@code roleId} is a plain UUID, without {@code @ManyToOne}: the {@code role} table belongs to the
 * {@code shifts} module. The foreign key exists only in the database (V7).
 */
@Entity
@Table(name = "worker", uniqueConstraints = @UniqueConstraint(
        name = WorkerEntity.UNIQUE_DOCUMENT_CONSTRAINT, columnNames = "document"))
@Getter
@Setter
@NoArgsConstructor
public class WorkerEntity {

    /** Name of the UNIQUE constraint on {@code document} (V7), used to recognize it when it is violated. */
    public static final String UNIQUE_DOCUMENT_CONSTRAINT = "uk_worker_document";

    @Id
    @UuidGenerator
    private UUID id;

    @Column(name = "full_name", nullable = false, length = 150)
    private String fullName;

    @Column(nullable = false, length = 20)
    private String document;

    @Column(name = "role_id", nullable = false)
    private UUID roleId;

    @Column(nullable = false)
    private boolean active;
}
