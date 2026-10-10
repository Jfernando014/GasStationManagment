package com.edu.unicauca.gasstation.backend.shifts.infrastructure.persistence.entities;

import com.edu.unicauca.gasstation.backend.shifts.domain.models.ShiftPeriod;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

/**
 * JPA mapping of the {@code shift_code} table (migration V6). Only used inside the persistence layer;
 * the schedule rules live in the domain model and in the table CHECK constraints.
 */
@Entity
@Table(name = "shift_code")
@Getter
@Setter
@NoArgsConstructor
public class ShiftCodeEntity {

    @Id
    @UuidGenerator
    private UUID id;

    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id")
    private RoleEntity role;

    @Enumerated(EnumType.STRING)
    private ShiftPeriod period;

    @Column(name = "start_hour_1")
    private Short startHour1;

    @Column(name = "end_hour_1")
    private Short endHour1;

    @Column(name = "start_hour_2")
    private Short startHour2;

    @Column(name = "end_hour_2")
    private Short endHour2;

    @Column(name = "requires_support", nullable = false)
    private boolean requiresSupport;

    /** Plain column, no recursive JPA relation. */
    @Column(name = "paired_with_id")
    private UUID pairedWithId;

    @Column(nullable = false)
    private boolean active;
}
