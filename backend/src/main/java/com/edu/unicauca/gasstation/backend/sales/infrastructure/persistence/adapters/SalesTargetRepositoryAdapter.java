package com.edu.unicauca.gasstation.backend.sales.infrastructure.persistence.adapters;

import com.edu.unicauca.gasstation.backend.sales.domain.models.*;
import com.edu.unicauca.gasstation.backend.sales.domain.repositories.SalesTargetRepository;
import com.edu.unicauca.gasstation.backend.sales.domain.services.SalesTargetStoragePolicy;
import com.edu.unicauca.gasstation.backend.sales.exception.DuplicateSalesTargetException;
import com.edu.unicauca.gasstation.backend.sales.infrastructure.persistence.entities.SalesTargetEntity;
import com.edu.unicauca.gasstation.backend.sales.infrastructure.persistence.mappers.SalesTargetPersistenceMapper;
import com.edu.unicauca.gasstation.backend.sales.infrastructure.persistence.repositories.JpaSalesTargetRepository;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SalesTargetRepositoryAdapter implements SalesTargetRepository {
    private final JpaSalesTargetRepository repository;
    private final SalesTargetPersistenceMapper mapper;
    private final EntityManager entityManager;

    @Override
    @Transactional
    public StoredSalesTarget register(SalesTarget target) {
        // Defense for callers of the port that bypass the service.
        SalesTargetStoragePolicy.validate(target.value());
        try {
            SalesTargetEntity saved = repository.saveAndFlush(mapper.toEntity(target));
            // Read the database default timestamp and the stored numeric representation.
            entityManager.refresh(saved);
            return mapper.toDomain(saved);
        } catch (DataIntegrityViolationException ex) {
            for (Throwable cause = ex; cause != null; cause = cause.getCause()) {
                if (cause instanceof ConstraintViolationException violation
                        && "23505".equals(violation.getSQLState())
                        && ("uq_sales_targets_annual".equals(violation.getConstraintName())
                        || "uq_sales_targets_monthly".equals(violation.getConstraintName()))) {
                    throw new DuplicateSalesTargetException(target.identity(), ex);
                }
            }
            throw ex;
        }
    }

    @Override
    public Optional<StoredSalesTarget> findById(Long id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<StoredSalesTarget> findByIdentity(SalesTargetIdentity identity) {
        SalesTargetPeriod period = identity.period();
        Short month = period.month() == null ? null : period.month().shortValue();
        return repository.findByTargetYearAndTargetMonthAndCategory(period.year(), month, identity.category())
                .map(mapper::toDomain);
    }

    @Override
    public List<StoredSalesTarget> findByYear(int year) {
        return repository.findByTargetYearOrderByTargetMonthAscCategoryAsc(year).stream().map(mapper::toDomain).toList();
    }
}
