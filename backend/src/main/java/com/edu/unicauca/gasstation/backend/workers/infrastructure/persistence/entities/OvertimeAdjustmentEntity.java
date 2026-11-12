package com.edu.unicauca.gasstation.backend.workers.infrastructure.persistence.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** JPA mapping of {@code worker_overtime_adjustment}. Only used inside the persistence layer. */
@Entity
@Table(name = "worker_overtime_adjustment")
@Getter
@Setter
@NoArgsConstructor
public class OvertimeAdjustmentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "worker_id", nullable = false)
    private UUID workerId;

    @Column(name = "fortnight_start", nullable = false)
    private LocalDate fortnightStart;

    @Column(nullable = false, length = 10)
    private String category;

    @Column(nullable = false, length = 10)
    private String direction;

    @Column(nullable = false)
    private int hours;

    @Column(nullable = false, length = 255)
    private String reason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "created_by", length = 100)
    private String createdBy;
}
