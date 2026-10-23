package com.edu.unicauca.gasstation.backend.competition.infrastructure.persistence.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "competitor")
@Getter @Setter @NoArgsConstructor
public class CompetitorEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 150)
    private String name;
    @Column(nullable = false, length = 100)
    private String brand;
    @Column(nullable = false, length = 255)
    private String location;
    @Column(nullable = false)
    private boolean active;
}
