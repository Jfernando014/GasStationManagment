package com.edu.unicauca.gasstation.backend.inventory.infrastructure.persistence.adapters;

import com.edu.unicauca.gasstation.backend.inventory.domain.models.StockBalance;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.StockLocation;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.StockMovement;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.StockMovementType;
import com.edu.unicauca.gasstation.backend.inventory.domain.repositories.StockMovementRepository;
import com.edu.unicauca.gasstation.backend.inventory.infrastructure.persistence.entities.ProductStockMovementEntity;
import com.edu.unicauca.gasstation.backend.inventory.infrastructure.persistence.mappers.StockMovementPersistenceMapper;
import com.edu.unicauca.gasstation.backend.inventory.infrastructure.persistence.repositories.JpaProductStockMovementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
public class StockMovementRepositoryAdapter implements StockMovementRepository {

    private final JpaProductStockMovementRepository repository;
    private final StockMovementPersistenceMapper mapper;

    @Override
    @Transactional
    public List<StockMovement> saveAll(List<StockMovement> movements) {
        List<ProductStockMovementEntity> entities = movements.stream().map(mapper::toEntity).toList();
        return repository.saveAll(entities).stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<StockMovement> findByJournalDate(LocalDate journalDate) {
        return repository.findByJournalDateOrderByIdAsc(journalDate).stream().map(mapper::toDomain).toList();
    }

    @Override
    public int sumQuantityUpTo(Long productId, StockLocation location, LocalDate date) {
        Long sum = repository.sumQuantityUpTo(productId, location, date);
        return sum == null ? 0 : sum.intValue();
    }

    @Override
    public List<StockBalance> findBalancesBefore(LocalDate date) {
        return repository.findBalancesBefore(date).stream()
                .map(row -> new StockBalance((Long) row[0], (StockLocation) row[1], ((Number) row[2]).intValue()))
                .toList();
    }

    @Override
    public boolean existsInitial(Long productId, StockLocation location) {
        return repository.existsByProductIdAndLocationAndMovementType(productId, location, StockMovementType.INITIAL);
    }

    @Override
    @Transactional
    public void replaceSales(LocalDate journalDate, List<StockMovement> sales) {
        repository.deleteByJournalDateAndMovementType(journalDate, StockMovementType.SALE);
        repository.saveAll(sales.stream().map(mapper::toEntity).toList());
        repository.flush();
    }
}