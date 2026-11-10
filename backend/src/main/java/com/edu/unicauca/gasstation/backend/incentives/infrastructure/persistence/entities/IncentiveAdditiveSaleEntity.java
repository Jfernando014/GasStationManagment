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

/** JPA mapping of {@code incentive_additive_sale}. Seller and product have no relationship: they belong to other modules. */
@Entity
@Table(name = "incentive_additive_sale")
@Getter
@Setter
@NoArgsConstructor
public class IncentiveAdditiveSaleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "journal_date", nullable = false)
    private LocalDate journalDate;

    @Column(name = "vendor_document", nullable = false, length = 30)
    private String vendorDocument;

    @Column(name = "vendor_name", nullable = false, length = 150)
    private String vendorName;

    @Column(nullable = false, length = 50)
    private String sku;

    @Column(name = "product_name", nullable = false, length = 150)
    private String productName;

    @Column(nullable = false)
    private int units;

    // Filled by the database
    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;
}
