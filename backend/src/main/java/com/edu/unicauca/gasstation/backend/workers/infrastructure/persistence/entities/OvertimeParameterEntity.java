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
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

/** JPA mapping of {@code worker_overtime_parameter} and the roles that have the fixed overtime switch on. */
@Entity
@Table(name = "worker_overtime_parameter")
@Getter
@Setter
@NoArgsConstructor
public class OvertimeParameterEntity {

    /** Name of the UNIQUE constraint on {@code valid_from}, used to recognize it when it is violated. */
    public static final String UNIQUE_VALID_FROM_CONSTRAINT = "uq_worker_overtime_parameter_valid_from";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "valid_from", nullable = false)
    private LocalDate validFrom;

    @Column(name = "night_start_hour", nullable = false)
    private int nightStartHour;

    @Column(name = "rest_hours", nullable = false)
    private int restHours;

    @Column(name = "rest_min_shift_hours", nullable = false)
    private int restMinShiftHours;

    @Column(name = "fixed_hedo_hours", nullable = false)
    private int fixedHedoHours;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "created_by", length = 100)
    private String createdBy;

    // Read with one extra query for the whole result, not one per version
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "worker_overtime_parameter_role", joinColumns = @JoinColumn(name = "parameter_id"))
    @Column(name = "role_name", nullable = false)
    @Fetch(FetchMode.SUBSELECT)
    private Set<String> fixedHedoRoles = new HashSet<>();
}
