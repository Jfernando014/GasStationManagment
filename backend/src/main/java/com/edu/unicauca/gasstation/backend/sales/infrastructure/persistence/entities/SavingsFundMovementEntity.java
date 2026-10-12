package com.edu.unicauca.gasstation.backend.sales.infrastructure.persistence.entities;

import com.edu.unicauca.gasstation.backend.sales.domain.models.SavingsFundMovementType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "savings_fund_movement")
public class SavingsFundMovementEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "movement_type", nullable = false, length = 20)
    private SavingsFundMovementType movementType;

    @Column(name = "movement_date", nullable = false)
    private LocalDate movementDate;

    @Column(name = "amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;

    @Column(name = "description", length = 255)
    private String description;

    protected SavingsFundMovementEntity() {
        // required by JPA
    }

    public SavingsFundMovementEntity(Long id, SavingsFundMovementType movementType, LocalDate movementDate,
                                     BigDecimal amount, String description) {
        this.id = id;
        this.movementType = movementType;
        this.movementDate = movementDate;
        this.amount = amount;
        this.description = description;
    }

    public Long getId() { return id; }
    public SavingsFundMovementType getMovementType() { return movementType; }
    public LocalDate getMovementDate() { return movementDate; }
    public BigDecimal getAmount() { return amount; }
    public String getDescription() { return description; }
}
