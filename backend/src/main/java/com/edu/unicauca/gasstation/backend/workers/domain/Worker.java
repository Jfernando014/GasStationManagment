package com.edu.unicauca.gasstation.backend.workers.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

/**
 * Seller of the station.
 *
 * <p>The role is stored as a plain {@code roleId}: the {@code role} table belongs to the {@code shifts}
 * module, so there is no {@code @ManyToOne} to it. The foreign key exists only in the database.
 * The dispenser and the products are derived from the role and are never stored here.
 *
 * <p>A worker is never deleted; it is deactivated so its sales, closings and shifts history is kept.
 */
@Entity
@Table(name = "worker", uniqueConstraints = @UniqueConstraint(
        name = Worker.UNIQUE_DOCUMENT_CONSTRAINT, columnNames = "document"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Worker {

    /** Name of the UNIQUE constraint on {@code document}, used to recognize it when it is violated. */
    public static final String UNIQUE_DOCUMENT_CONSTRAINT = "uk_worker_document";

    @Id
    @UuidGenerator
    private UUID id;

    @Column(name = "full_name", nullable = false, length = 150)
    private String fullName;

    /** Identity document (cédula). Text, not a number: it may have leading zeros. */
    @Column(nullable = false, length = 20)
    private String document;

    @Column(name = "role_id", nullable = false)
    private UUID roleId;

    @Column(nullable = false)
    private boolean active;

    /**
     * Creates a new worker. Every new worker starts active.
     */
    public Worker(String fullName, String document, UUID roleId) {
        this.fullName = fullName;
        this.document = document;
        this.roleId = roleId;
        this.active = true;
    }

    /** Replaces the editable data. Allowed even when the worker is inactive. */
    public void updateDetails(String fullName, String document, UUID roleId) {
        this.fullName = fullName;
        this.document = document;
        this.roleId = roleId;
    }

    /** Marks the worker as active. Does nothing if it already is. */
    public void activate() {
        this.active = true;
    }

    /** Marks the worker as inactive. Does nothing if it already is. */
    public void deactivate() {
        this.active = false;
    }
}
