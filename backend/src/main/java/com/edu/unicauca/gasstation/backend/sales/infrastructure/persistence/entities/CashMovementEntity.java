package com.edu.unicauca.gasstation.backend.sales.infrastructure.persistence.entities;

import com.edu.unicauca.gasstation.backend.sales.domain.models.CashMovementType;
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
@Table(name = "cash_movement")
public class CashMovementEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "movement_type", nullable = false, length = 20)
    private CashMovementType movementType;

    @Column(name = "movement_date", nullable = false)
    private LocalDate movementDate;

    @Column(name = "amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;

    @Column(name = "description", length = 255)
    private String description;

    protected CashMovementEntity() {
        // required by JPA
    }

    public CashMovementEntity(Long id, CashMovementType movementType, LocalDate movementDate,
                              BigDecimal amount, String description) {
        this.id = id;
        this.movementType = movementType;
        this.movementDate = movementDate;
        this.amount = amount;
        this.description = description;
    }

    public Long getId() { return id; }
    public CashMovementType getMovementType() { return movementType; }
    public LocalDate getMovementDate() { return movementDate; }
    public BigDecimal getAmount() { return amount; }
    public String getDescription() { return description; }
}
