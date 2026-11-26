package com.edu.unicauca.gasstation.backend.sales.infrastructure.persistence.adapters;

import com.edu.unicauca.gasstation.backend.sales.domain.models.DiscrepancyStatus;
import com.edu.unicauca.gasstation.backend.sales.domain.models.VendorCloseProduct;
import com.edu.unicauca.gasstation.backend.sales.domain.models.VendorShiftClose;
import com.edu.unicauca.gasstation.backend.sales.domain.repositories.VendorShiftCloseRepository;
import com.edu.unicauca.gasstation.backend.sales.infrastructure.persistence.entities.VendorCloseProductEntity;
import com.edu.unicauca.gasstation.backend.sales.infrastructure.persistence.entities.VendorShiftCloseEntity;
import com.edu.unicauca.gasstation.backend.sales.infrastructure.persistence.repositories.VendorShiftCloseJpaRepository;
import java.text.Normalizer;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
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
        return saveAll(List.of(close)).get(0);
    }

    @Override
    @Transactional
    public List<VendorShiftClose> saveAll(List<VendorShiftClose> closes) {
        if (closes == null || closes.isEmpty()) {
            return List.of();
        }

        // Deduplicate within the incoming batch by unique key (journalDate, vendorDocument, shiftName).
        Map<CloseKey, VendorShiftClose> deduped = new LinkedHashMap<>();
        for (VendorShiftClose close : closes) {
            deduped.put(new CloseKey(close.journalDate(), close.vendorDocument(), close.shiftName()), close);
        }

        Map<LocalDate, List<VendorShiftCloseEntity>> existingByDate = new HashMap<>();
        for (VendorShiftClose close : deduped.values()) {
            existingByDate.computeIfAbsent(close.journalDate(),
                    date -> new ArrayList<>(jpaRepository.findByJournalDateOrderByShiftNameAscPumpNumberAsc(date)));
        }

        Set<UUID> claimedIds = new HashSet<>();
        List<VendorShiftCloseEntity> entities = new ArrayList<>(deduped.size());
        for (VendorShiftClose domain : deduped.values()) {
            List<VendorShiftCloseEntity> existingForDate = existingByDate.getOrDefault(domain.journalDate(), List.of());
            VendorShiftCloseEntity target = findMatchingEntity(domain, existingForDate, claimedIds);
            if (target != null) {
                if (target.getId() != null) {
                    claimedIds.add(target.getId());
                }
                applyDomain(target, domain);
                entities.add(target);
            } else {
                entities.add(toEntity(domain));
            }
        }

        List<VendorShiftCloseEntity> saved = jpaRepository.saveAllAndFlush(entities);
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
    public List<VendorShiftClose> findByJournalDateBetween(LocalDate from, LocalDate to) {
        return jpaRepository.findByJournalDateBetweenOrderByJournalDateAscShiftNameAscPumpNumberAsc(from, to)
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

    @Override
    @Transactional
    public void deleteByJournalDate(LocalDate journalDate) {
        if (journalDate == null) {
            return;
        }
        List<VendorShiftCloseEntity> existing = jpaRepository.findByJournalDateOrderByShiftNameAscPumpNumberAsc(journalDate);
        if (!existing.isEmpty()) {
            jpaRepository.deleteAll(existing);
            jpaRepository.flush();
        }
    }

    private VendorShiftCloseEntity findMatchingEntity(
            VendorShiftClose domain,
            List<VendorShiftCloseEntity> existingForDate,
            Set<UUID> claimedIds) {
        if (domain.id() != null) {
            for (VendorShiftCloseEntity entity : existingForDate) {
                if (domain.id().equals(entity.getId()) && !claimedIds.contains(entity.getId())) {
                    return entity;
                }
            }
        }
        // Exact match on uq_vendor_shift_close (journal_date, vendor_document, shift_name)
        for (VendorShiftCloseEntity entity : existingForDate) {
            if (claimedIds.contains(entity.getId())) {
                continue;
            }
            if (domain.vendorDocument().equals(entity.getVendorDocument())
                    && domain.shiftName().equals(entity.getShiftName())) {
                return entity;
            }
        }
        // Fallback match by normalized (vendorName, shiftName) to seamlessly update legacy cédula rows to worker UUIDs
        String normalizedName = normalizeName(domain.vendorName());
        if (!normalizedName.isEmpty()) {
            for (VendorShiftCloseEntity entity : existingForDate) {
                if (claimedIds.contains(entity.getId())) {
                    continue;
                }
                if (normalizedName.equals(normalizeName(entity.getVendorName()))
                        && domain.shiftName().equals(entity.getShiftName())) {
                    return entity;
                }
            }
        }
        return null;
    }

    private void applyDomain(VendorShiftCloseEntity entity, VendorShiftClose domain) {
        entity.setJournalDate(domain.journalDate());
        entity.setVendorDocument(domain.vendorDocument());
        entity.setVendorName(domain.vendorName());
        entity.setShiftName(domain.shiftName());
        entity.setPumpNumber(domain.pumpNumber());
        entity.setCash(domain.cash());
        entity.setGa(domain.ga());
        entity.setCredibanco(domain.credibanco());
        entity.setQr(domain.qr());
        entity.setRedemptions(domain.redemptions());
        entity.setTotalDelivered(domain.totalDelivered());
        entity.setGallonsMotor(domain.gallonsMotor());
        entity.setGallonsDiesel(domain.gallonsDiesel());
        entity.setGallonsExtra(domain.gallonsExtra());
        entity.setGallonsMaxpro(domain.gallonsMaxpro());
        entity.setBasketDominusAmount(domain.basketDominusAmount());
        entity.setDominusTirilla(domain.dominusTirilla());
        entity.setRealDiscrepancy(domain.realDiscrepancy());
        entity.setAppliedTolerance(domain.appliedTolerance());
        entity.setResultStatus(domain.resultStatus() != null ? domain.resultStatus().name() : DiscrepancyStatus.CUADRA.name());

        if (entity.getProducts() == null) {
            entity.setProducts(new ArrayList<>());
        } else {
            entity.getProducts().clear();
        }
        if (domain.products() != null) {
            for (VendorCloseProduct p : domain.products()) {
                VendorCloseProductEntity pe = VendorCloseProductEntity.builder()
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
    }

    private static String normalizeName(String name) {
        if (name == null || name.isBlank()) {
            return "";
        }
        String decomposed = Normalizer.normalize(name.strip(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");
        return decomposed.replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    private record CloseKey(LocalDate journalDate, String vendorDocument, String shiftName) {}

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
