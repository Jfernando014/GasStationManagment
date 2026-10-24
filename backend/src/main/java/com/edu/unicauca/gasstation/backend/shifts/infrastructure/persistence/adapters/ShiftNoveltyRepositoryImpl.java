package com.edu.unicauca.gasstation.backend.shifts.infrastructure.persistence.adapters;

import com.edu.unicauca.gasstation.backend.shifts.domain.models.ShiftNovelty;
import com.edu.unicauca.gasstation.backend.shifts.domain.repositories.ShiftNoveltyRepository;
import com.edu.unicauca.gasstation.backend.shifts.infrastructure.persistence.entities.ShiftNoveltyEntity;
import com.edu.unicauca.gasstation.backend.shifts.infrastructure.persistence.mappers.ShiftNoveltyPersistenceMapper;
import com.edu.unicauca.gasstation.backend.shifts.infrastructure.persistence.repositories.JpaShiftCodeRepository;
import com.edu.unicauca.gasstation.backend.shifts.infrastructure.persistence.repositories.JpaShiftNoveltyRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implements the domain {@link ShiftNoveltyRepository} with Spring Data JPA.
 */
@Repository
@RequiredArgsConstructor
public class ShiftNoveltyRepositoryImpl implements ShiftNoveltyRepository {

    private final JpaShiftNoveltyRepository repository;
    private final JpaShiftCodeRepository shiftCodeRepository;
    private final ShiftNoveltyPersistenceMapper mapper;

    @Override
    @Transactional
    public ShiftNovelty save(ShiftNovelty novelty) {
        ShiftNoveltyEntity saved = repository.save(mapper.toEntity(novelty,
                shiftCodeRepository.getReferenceById(novelty.getOriginalShiftCode().getId()),
                novelty.getFinalShiftCode() == null ? null
                        : shiftCodeRepository.getReferenceById(novelty.getFinalShiftCode().getId())));
        // Built from the saved domain object, so the shift code references do not have to be loaded again
        return ShiftNovelty.restore(saved.getId(), novelty.getWorkDate(), novelty.getType(), novelty.getWorkerId(),
                novelty.getCounterpartWorkerId(), novelty.getOriginalShiftCode(), novelty.getFinalShiftCode(),
                novelty.isExceptionalCoverage(), novelty.getReason(), novelty.getCreatedAt(), novelty.getCancelledAt());
    }

    @Override
    public Optional<ShiftNovelty> findById(UUID id) {
        return repository.findWithCodesById(id).map(mapper::toDomain);
    }

    @Override
    public List<ShiftNovelty> findByWorkDateBetween(LocalDate from, LocalDate to) {
        return repository.findByWorkDateBetweenAndCancelledAtIsNullOrderByCreatedAtAscIdAsc(from, to).stream()
                .map(mapper::toDomain)
                .toList();
    }
}
