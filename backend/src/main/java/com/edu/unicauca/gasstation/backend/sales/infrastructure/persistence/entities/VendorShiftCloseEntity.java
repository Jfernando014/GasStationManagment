package com.edu.unicauca.gasstation.backend.sales.infrastructure.persistence.entities;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "vendor_shift_closes",
        uniqueConstraints = @UniqueConstraint(name = "uq_vendor_shift_close", columnNames = {"journal_date", "vendor_document", "shift_name"})
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VendorShiftCloseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "journal_date", nullable = false)
    private LocalDate journalDate;

    @Column(name = "vendor_document", nullable = false, length = 30)
    private String vendorDocument;

    @Column(name = "vendor_name", nullable = false, length = 150)
    private String vendorName;

    @Column(name = "shift_name", nullable = false, length = 50)
    private String shiftName;

    @Column(name = "pump_number")
    private Integer pumpNumber;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal cash;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal ga;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal credibanco;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal qr;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal redemptions;

    @Column(name = "total_delivered", nullable = false, precision = 14, scale = 2)
    private BigDecimal totalDelivered;

    @Column(name = "gallons_motor", nullable = false, precision = 12, scale = 3)
    private BigDecimal gallonsMotor;

    @Column(name = "gallons_diesel", nullable = false, precision = 12, scale = 3)
    private BigDecimal gallonsDiesel;

    @Column(name = "gallons_extra", nullable = false, precision = 12, scale = 3)
    private BigDecimal gallonsExtra;

    @Column(name = "gallons_maxpro", nullable = false, precision = 12, scale = 3)
    private BigDecimal gallonsMaxpro;

    @Column(name = "basket_dominus_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal basketDominusAmount;

    @Column(name = "dominus_tirilla", nullable = false, precision = 14, scale = 2)
    private BigDecimal dominusTirilla;

    @Column(name = "real_discrepancy", nullable = false, precision = 14, scale = 2)
    private BigDecimal realDiscrepancy;

    @Column(name = "applied_tolerance", nullable = false, precision = 10, scale = 2)
    private BigDecimal appliedTolerance;

    @Column(name = "result_status", nullable = false, length = 20)
    private String resultStatus;

    @Builder.Default
    @OneToMany(mappedBy = "vendorShiftClose", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<VendorCloseProductEntity> products = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public void addProduct(VendorCloseProductEntity product) {
        if (products == null) {
            products = new ArrayList<>();
        }
        products.add(product);
        product.setVendorShiftClose(this);
    }
}
