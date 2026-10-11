package com.edu.unicauca.gasstation.backend.sales.infrastructure.persistence.entities;

import com.edu.unicauca.gasstation.backend.sales.domain.models.SalesTargetCategory;
import com.edu.unicauca.gasstation.backend.sales.domain.models.SalesTargetUnit;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** V9 mapping. Partial unique indexes are owned exclusively by Flyway. */
@Entity
@Table(name = "sales_targets")
@Getter
@Setter
@NoArgsConstructor
public class SalesTargetEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "target_year", nullable = false, updatable = false)
    private int targetYear;

    @Column(name = "target_month", updatable = false)
    private Short targetMonth;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30, updatable = false)
    private SalesTargetCategory category;

    @Column(name = "target_value", nullable = false, precision = 30, scale = 12, updatable = false)
    private BigDecimal targetValue;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10, updatable = false)
    private SalesTargetUnit unit;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;
}
