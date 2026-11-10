package com.edu.unicauca.gasstation.backend.inventory.infrastructure.persistence.adapters;

import com.edu.unicauca.gasstation.backend.inventory.InspectionCategory;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.Inspection;
import com.edu.unicauca.gasstation.backend.inventory.domain.repositories.InspectionRepository;
import com.edu.unicauca.gasstation.backend.inventory.infrastructure.persistence.mappers.InspectionPersistenceMapper;
import com.edu.unicauca.gasstation.backend.inventory.infrastructure.persistence.repositories.JpaInspectionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class InspectionRepositoryAdapter implements InspectionRepository {

    private static final LocalDate MIN_DATE = LocalDate.of(1900, 1, 1);
    private static final LocalDate MAX_DATE = LocalDate.of(9999, 12, 31);

    private final JpaInspectionRepository repository;
    private final InspectionPersistenceMapper mapper;

    @Override
    public Inspection save(Inspection inspection) {
        return mapper.toDomain(repository.save(mapper.toEntity(inspection)));
    }

    @Override
    public Optional<Inspection> findById(Long id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<Inspection> findByCode(String code) {
        return repository.findByCode(code).map(mapper::toDomain);
    }

    @Override
    public boolean existsByCode(String code) {
        return repository.existsByCode(code);
    }

    @Override
    public boolean existsByCodeAndIdNot(String code, Long id) {
        return repository.existsByCodeAndIdNot(code, id);
    }

    @Override
    public List<Inspection> search(InspectionCategory category, LocalDate from, LocalDate to) {
        LocalDate fromDate = from != null ? from : MIN_DATE;
        LocalDate toDate = to != null ? to : MAX_DATE;
        return repository.search(category, fromDate, toDate).stream().map(mapper::toDomain).toList();
    }
}
