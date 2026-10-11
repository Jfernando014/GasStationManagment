package com.edu.unicauca.gasstation.backend.sales.domain.services;

import com.edu.unicauca.gasstation.backend.sales.domain.models.*;
import com.edu.unicauca.gasstation.backend.sales.domain.repositories.SalesTargetRepository;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SalesTargetService {
    private final SalesTargetRepository repository;

    @Transactional
    public StoredSalesTarget register(SalesTarget target) {
        Objects.requireNonNull(target, "target");
        SalesTargetStoragePolicy.validate(target.value());
        if (target.maximumSale() != null) SalesTargetStoragePolicy.validate(target.maximumSale());
        return repository.register(target);
    }

    @Transactional(readOnly = true)
    public Optional<StoredSalesTarget> findById(Long id) {
        return repository.findById(Objects.requireNonNull(id, "id"));
    }

    @Transactional(readOnly = true)
    public Optional<StoredSalesTarget> findByIdentity(SalesTargetIdentity identity) {
        return repository.findByIdentity(Objects.requireNonNull(identity, "identity"));
    }

    @Transactional(readOnly = true)
    public List<StoredSalesTarget> findByYear(int year) {
        return repository.findByYear(year);
    }
}
