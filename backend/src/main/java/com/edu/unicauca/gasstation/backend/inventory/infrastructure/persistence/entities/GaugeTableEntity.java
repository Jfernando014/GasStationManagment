package com.edu.unicauca.gasstation.backend.inventory.infrastructure.persistence.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "gauge_table",
        uniqueConstraints = @UniqueConstraint(name = "uq_gauge_table_name", columnNames = "name"))
@Getter
@Setter
@NoArgsConstructor
public class GaugeTableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 80)
    private String name;
}