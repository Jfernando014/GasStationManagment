package com.edu.unicauca.gasstation.backend.inventory.infrastructure.persistence.entities;

import com.edu.unicauca.gasstation.backend.inventory.InspectionCategory;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "inspection",
        uniqueConstraints = @UniqueConstraint(name = "uq_inspection_code", columnNames = "code"))
@Getter
@Setter
@NoArgsConstructor
public class InspectionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", nullable = false, length = 20)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 20)
    private InspectionCategory category;

    @Column(name = "target", nullable = false, length = 150)
    private String target;

    @Column(name = "date_executed", nullable = false)
    private LocalDate dateExecuted;

    @Column(name = "date_due", nullable = false)
    private LocalDate dateDue;

    @Column(name = "certifier", nullable = false, length = 150)
    private String certifier;

    @Column(name = "tolerance_measured", nullable = false, length = 100)
    private String toleranceMeasured;

    @Column(name = "cert_doc", length = 200)
    private String certDoc;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
