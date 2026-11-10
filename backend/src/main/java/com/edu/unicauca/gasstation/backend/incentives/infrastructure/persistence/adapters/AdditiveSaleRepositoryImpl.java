package com.edu.unicauca.gasstation.backend.incentives.infrastructure.persistence.adapters;

import com.edu.unicauca.gasstation.backend.incentives.domain.models.AdditiveSaleUnits;
import com.edu.unicauca.gasstation.backend.incentives.domain.repositories.AdditiveSaleRepository;
import com.edu.unicauca.gasstation.backend.incentives.infrastructure.persistence.entities.IncentiveAdditiveSaleEntity;
import com.edu.unicauca.gasstation.backend.incentives.infrastructure.persistence.repositories.JpaAdditiveSaleRepository;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/** JPA implementation of {@link AdditiveSaleRepository}. */
@Repository
@RequiredArgsConstructor
public class AdditiveSaleRepositoryImpl implements AdditiveSaleRepository {

    private final JpaAdditiveSaleRepository repository;

    @Override
    @Transactional
    public void replaceDay(LocalDate journalDate, List<AdditiveSaleUnits> sales) {
        repository.deleteByJournalDate(journalDate);
        repository.saveAllAndFlush(sales.stream().map(AdditiveSaleRepositoryImpl::toEntity).toList());
    }

    @Override
    public List<AdditiveSaleUnits> findByDate(LocalDate journalDate) {
        return repository.findByJournalDateOrderByVendorNameAscVendorDocumentAscSkuAsc(journalDate).stream()
                .map(AdditiveSaleRepositoryImpl::toDomain)
                .toList();
    }

    private static AdditiveSaleUnits toDomain(IncentiveAdditiveSaleEntity entity) {
        return new AdditiveSaleUnits(entity.getJournalDate(), entity.getVendorDocument(), entity.getVendorName(),
                entity.getSku(), entity.getProductName(), entity.getUnits());
    }

    private static IncentiveAdditiveSaleEntity toEntity(AdditiveSaleUnits sale) {
        IncentiveAdditiveSaleEntity entity = new IncentiveAdditiveSaleEntity();
        entity.setJournalDate(sale.journalDate());
        entity.setVendorDocument(sale.vendorDocument());
        entity.setVendorName(sale.vendorName());
        entity.setSku(sale.sku());
        entity.setProductName(sale.productName());
        entity.setUnits(sale.units());
        return entity;
    }
}
