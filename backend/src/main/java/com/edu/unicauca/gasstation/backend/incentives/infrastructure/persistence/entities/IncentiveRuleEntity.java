package com.edu.unicauca.gasstation.backend.incentives.infrastructure.persistence.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** JPA mapping of {@code incentive_rule}. {@code productId} has no relationship: products belong to inventory. */
@Entity
@Table(name = "incentive_rule")
@Getter
@Setter
@NoArgsConstructor
public class IncentiveRuleEntity {

    /** Name of the unique index on {@code product_id} and {@code valid_from} (V27), used to recognize it. */
    public static final String UNIQUE_PRODUCT_VALID_FROM_CONSTRAINT = "uq_incentive_rule_product_valid_from";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "value_per_unit", nullable = false)
    private int valuePerUnit;

    @Column(name = "valid_from", nullable = false)
    private LocalDate validFrom;

    @Column(nullable = false)
    private boolean active;

    @Column(length = 255)
    private String reason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
