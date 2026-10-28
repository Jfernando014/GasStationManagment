package com.edu.unicauca.gasstation.backend.shifts.infrastructure.persistence.entities;

import com.edu.unicauca.gasstation.backend.shifts.daystaff.DayStaff.NoveltyType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

/**
 * JPA mapping of the {@code shift_novelty} table (migration V23). Only used inside the persistence layer.
 *
 * <p>{@code workerId} and {@code counterpartWorkerId} are plain UUIDs, without {@code @ManyToOne}: workers belong to
 * the {@code workers} module. The foreign keys exist only in the database (V23).
 */
@Entity
@Table(name = "shift_novelty")
@Getter
@Setter
@NoArgsConstructor
public class ShiftNoveltyEntity {

    @Id
    @UuidGenerator
    private UUID id;

    @Column(name = "work_date", nullable = false)
    private LocalDate workDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NoveltyType type;

    @Column(name = "worker_id", nullable = false)
    private UUID workerId;

    @Column(name = "counterpart_worker_id")
    private UUID counterpartWorkerId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "original_shift_code_id", nullable = false)
    private ShiftCodeEntity originalShiftCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "final_shift_code_id")
    private ShiftCodeEntity finalShiftCode;

    @Column(name = "exceptional_coverage", nullable = false)
    private boolean exceptionalCoverage;

    @Column(nullable = false, length = 255)
    private String reason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;
}
