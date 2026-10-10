package com.edu.unicauca.gasstation.backend.shifts.infrastructure.persistence.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

/**
 * JPA mapping of the {@code shift_assignment} table (migration V8). Only used inside the persistence layer.
 *
 * <p>{@code workerId} is a plain UUID, without {@code @ManyToOne}: workers belong to the {@code workers} module.
 * The foreign key exists only in the database (V8).
 */
@Entity
@Table(name = "shift_assignment", uniqueConstraints = @UniqueConstraint(
        name = "uk_shift_assignment_worker_date", columnNames = {"worker_id", "work_date"}))
@Getter
@Setter
@NoArgsConstructor
public class ShiftAssignmentEntity {

    @Id
    @UuidGenerator
    private UUID id;

    @Column(name = "worker_id", nullable = false)
    private UUID workerId;

    @Column(name = "work_date", nullable = false)
    private LocalDate workDate;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shift_code_id", nullable = false)
    private ShiftCodeEntity shiftCode;

    @Column(length = 255)
    private String note;
}
