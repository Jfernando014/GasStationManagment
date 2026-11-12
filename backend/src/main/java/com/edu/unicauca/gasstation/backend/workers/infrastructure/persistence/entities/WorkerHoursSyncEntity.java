package com.edu.unicauca.gasstation.backend.workers.infrastructure.persistence.entities;

import com.edu.unicauca.gasstation.backend.workers.domain.models.SyncStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** JPA mapping of {@code worker_hours_sync}. The journal data belongs to sales, so it has no relationship. */
@Entity
@Table(name = "worker_hours_sync")
@Getter
@Setter
@NoArgsConstructor
public class WorkerHoursSyncEntity {

    @Id
    @Column(name = "work_date")
    private LocalDate workDate;

    @Column(name = "journal_id")
    private Long journalId;

    private Long consecutive;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private SyncStatus status;

    @Column(name = "novelties_applied", nullable = false)
    private boolean noveltiesApplied;

    @Column(nullable = false)
    private int attempts;

    @Column(name = "last_error", length = 500)
    private String lastError;

    @Column(name = "processed_at")
    private Instant processedAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
