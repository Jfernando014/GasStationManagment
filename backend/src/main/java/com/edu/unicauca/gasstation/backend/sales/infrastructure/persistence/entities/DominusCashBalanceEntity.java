package com.edu.unicauca.gasstation.backend.sales.infrastructure.persistence.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "dominus_cash_balance")
public class DominusCashBalanceEntity {

    @Id
    @Column(name = "business_date", nullable = false)
    private LocalDate businessDate;

    @Column(name = "accumulated_balance", nullable = false, precision = 14, scale = 2)
    private BigDecimal accumulatedBalance;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected DominusCashBalanceEntity() {
        // required by JPA
    }

    public DominusCashBalanceEntity(LocalDate businessDate, BigDecimal accumulatedBalance, Instant updatedAt) {
        this.businessDate = businessDate;
        this.accumulatedBalance = accumulatedBalance;
        this.updatedAt = updatedAt;
    }

    public LocalDate getBusinessDate() { return businessDate; }
    public BigDecimal getAccumulatedBalance() { return accumulatedBalance; }
    public Instant getUpdatedAt() { return updatedAt; }
}
