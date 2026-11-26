package com.edu.unicauca.gasstation.backend.inventory.infrastructure.persistence.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "variation_alert_setting")
@Getter
@Setter
@NoArgsConstructor
public class VariationAlertSettingEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "valid_from", nullable = false, unique = true)
    private LocalDate validFrom;

    @Column(name = "cumulative_limit_percentage", nullable = false, precision = 7, scale = 4)
    private BigDecimal cumulativeLimitPercentage;

    @Column(name = "escalation_days", nullable = false)
    private Integer escalationDays;

    @Column(name = "shortage_limit_1_to_49", nullable = false)
    private Integer shortageLimit1To49;

    @Column(name = "shortage_limit_50_to_74", nullable = false)
    private Integer shortageLimit50To74;

    @Column(name = "shortage_limit_75_to_124", nullable = false)
    private Integer shortageLimit75To124;

    @Column(name = "shortage_limit_125_to_249", nullable = false)
    private Integer shortageLimit125To249;

    @Column(name = "shortage_limit_250_to_499", nullable = false)
    private Integer shortageLimit250To499;

    @Column(name = "shortage_limit_500_or_more", nullable = false)
    private Integer shortageLimit500OrMore;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
