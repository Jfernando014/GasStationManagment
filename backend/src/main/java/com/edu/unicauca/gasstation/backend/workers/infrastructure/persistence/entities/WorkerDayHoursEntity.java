package com.edu.unicauca.gasstation.backend.workers.infrastructure.persistence.entities;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

/** JPA mapping of {@code worker_day_hours} and its segments. Only used inside the persistence layer. */
@Entity
@Table(name = "worker_day_hours")
@Getter
@Setter
@NoArgsConstructor
public class WorkerDayHoursEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "work_date", nullable = false)
    private LocalDate workDate;

    @Column(name = "worker_id", nullable = false)
    private UUID workerId;

    @Column(name = "worker_name", nullable = false, length = 150)
    private String workerName;

    @Column(name = "worked_code")
    private String workedCode;

    @Column(name = "grid_code", nullable = false)
    private String gridCode;

    @Column(name = "day_role")
    private String dayRole;

    @Column(name = "total_hours", nullable = false)
    private int totalHours;

    @Column(name = "novelty_type", nullable = false, length = 20)
    private String noveltyType;

    @Column(name = "replaced_worker_id")
    private UUID replacedWorkerId;

    @Column(name = "replaced_worker_name", length = 150)
    private String replacedWorkerName;

    @Column(length = 255)
    private String reason;

    @Column(name = "journal_id")
    private Long journalId;

    @Column(name = "novelties_applied", nullable = false)
    private boolean noveltiesApplied;

    // Filled by the database
    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    // Read with one extra query for the whole result, not one per record
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "worker_day_hours_segment", joinColumns = @JoinColumn(name = "day_hours_id"))
    @OrderColumn(name = "position")
    @Fetch(FetchMode.SUBSELECT)
    private List<DayHoursSegmentEmbeddable> segments = new ArrayList<>();
}
