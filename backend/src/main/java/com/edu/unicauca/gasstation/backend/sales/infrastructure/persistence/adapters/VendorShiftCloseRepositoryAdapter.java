package com.edu.unicauca.gasstation.backend.sales.infrastructure.persistence.adapters;

import com.edu.unicauca.gasstation.backend.sales.domain.models.DiscrepancyStatus;
import com.edu.unicauca.gasstation.backend.sales.domain.models.VendorCloseProduct;
import com.edu.unicauca.gasstation.backend.sales.domain.models.VendorShiftClose;
import com.edu.unicauca.gasstation.backend.sales.domain.repositories.VendorShiftCloseRepository;
import com.edu.unicauca.gasstation.backend.sales.infrastructure.persistence.entities.VendorCloseProductEntity;
import com.edu.unicauca.gasstation.backend.sales.infrastructure.persistence.entities.VendorShiftCloseEntity;
import com.edu.unicauca.gasstation.backend.sales.infrastructure.persistence.repositories.VendorShiftCloseJpaRepository;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class VendorShiftCloseRepositoryAdapter implements VendorShiftCloseRepository {

    private final VendorShiftCloseJpaRepository jpaRepository;

    @Override
    @Transactional
    public VendorShiftClose save(VendorShiftClose close) {
        VendorShiftCloseEntity entity = toEntity(close);
        VendorShiftCloseEntity saved = jpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    @Transactional
    public List<VendorShiftClose> saveAll(List<VendorShiftClose> closes) {
        List<VendorShiftCloseEntity> entities = closes.stream().map(this::toEntity).toList();
        List<VendorShiftCloseEntity> saved = jpaRepository.saveAll(entities);
        return saved.stream().map(this::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<VendorShiftClose> findByJournalDate(LocalDate journalDate) {
        return jpaRepository.findByJournalDateOrderByShiftNameAscPumpNumberAsc(journalDate)
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<VendorShiftClose> findByJournalDateAndVendorDocument(LocalDate journalDate, String vendorDocument) {
        return jpaRepository.findByJournalDateAndVendorDocument(journalDate, vendorDocument)
                .map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByJournalDate(LocalDate journalDate) {
        return jpaRepository.existsByJournalDate(journalDate);
    }

    private VendorShiftCloseEntity toEntity(VendorShiftClose domain) {
        VendorShiftCloseEntity entity = VendorShiftCloseEntity.builder()
                .id(domain.id())
                .journalDate(domain.journalDate())
                .vendorDocument(domain.vendorDocument())
                .vendorName(domain.vendorName())
                .shiftName(domain.shiftName())
                .pumpNumber(domain.pumpNumber())
                .cash(domain.cash())
                .ga(domain.ga())
                .credibanco(domain.credibanco())
                .qr(domain.qr())
                .redemptions(domain.redemptions())
                .totalDelivered(domain.totalDelivered())
                .gallonsMotor(domain.gallonsMotor())
                .gallonsDiesel(domain.gallonsDiesel())
                .gallonsExtra(domain.gallonsExtra())
                .gallonsMaxpro(domain.gallonsMaxpro())
                .basketDominusAmount(domain.basketDominusAmount())
                .dominusTirilla(domain.dominusTirilla())
                .realDiscrepancy(domain.realDiscrepancy())
                .appliedTolerance(domain.appliedTolerance())
                .resultStatus(domain.resultStatus() != null ? domain.resultStatus().name() : DiscrepancyStatus.CUADRA.name())
                .products(new ArrayList<>())
                .build();

        if (domain.products() != null) {
            for (VendorCloseProduct p : domain.products()) {
                VendorCloseProductEntity pe = VendorCloseProductEntity.builder()
                        .id(p.id())
                        .sku(p.sku())
                        .productName(p.productName())
                        .category(p.category())
                        .unitsSold(p.unitsSold())
                        .unitPrice(p.unitPrice())
                        .totalAmount(p.totalAmount())
                        .build();
                entity.addProduct(pe);
            }
        }
        return entity;
    }

    private VendorShiftClose toDomain(VendorShiftCloseEntity entity) {
        List<VendorCloseProduct> products = entity.getProducts() != null
                ? entity.getProducts().stream()
                .map(p -> new VendorCloseProduct(
                        p.getId(),
                        p.getSku(),
                        p.getProductName(),
                        p.getCategory(),
                        p.getUnitsSold(),
                        p.getUnitPrice(),
                        p.getTotalAmount()
                )).toList()
                : List.of();

        DiscrepancyStatus status;
        try {
            status = DiscrepancyStatus.valueOf(entity.getResultStatus());
        } catch (Exception e) {
            status = DiscrepancyStatus.CUADRA;
        }

        return new VendorShiftClose(
                entity.getId(),
                entity.getJournalDate(),
                entity.getVendorDocument(),
                entity.getVendorName(),
                entity.getShiftName(),
                entity.getPumpNumber(),
                entity.getCash(),
                entity.getGa(),
                entity.getCredibanco(),
                entity.getQr(),
                entity.getRedemptions(),
                entity.getTotalDelivered(),
                entity.getGallonsMotor(),
                entity.getGallonsDiesel(),
                entity.getGallonsExtra(),
                entity.getGallonsMaxpro(),
                entity.getBasketDominusAmount(),
                entity.getDominusTirilla(),
                entity.getRealDiscrepancy(),
                entity.getAppliedTolerance(),
                status,
                products
        );
    }
}
