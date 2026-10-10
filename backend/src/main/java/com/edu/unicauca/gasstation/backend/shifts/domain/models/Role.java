package com.edu.unicauca.gasstation.backend.shifts.domain.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

/**
 * Role a worker plays in a shift (TITULAR or APOYO). Mapped to the {@code role} table (migration V6).
 * The dispenser is derived from the role, not stored on the worker.
 */
@Entity
@Table(name = "role")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Role {

    @Id
    @UuidGenerator
    private UUID id;

    @Column(nullable = false, unique = true)
    private String name;

    /** Dispenser the role works on (TITULAR = 1, APOYO = 3). */
    @Column(nullable = false)
    private int dispenser;

    public Role(String name, int dispenser) {
        this.name = name;
        this.dispenser = dispenser;
    }
}
