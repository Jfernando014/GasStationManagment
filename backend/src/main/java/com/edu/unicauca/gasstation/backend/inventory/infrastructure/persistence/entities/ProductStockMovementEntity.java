package com.edu.unicauca.gasstation.backend.inventory.infrastructure.persistence.entities;

import com.edu.unicauca.gasstation.backend.inventory.domain.models.StockLocation;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.StockMovementType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "product_stock_movement")
@Getter
@Setter
@NoArgsConstructor
public class ProductStockMovementEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "journal_date", nullable = false)
    private LocalDate journalDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "location", nullable = false, length = 10)
    private StockLocation location;

    @Enumerated(EnumType.STRING)
    @Column(name = "movement_type", nullable = false, length = 12)
    private StockMovementType movementType;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "note", length = 200)
    private String note;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}