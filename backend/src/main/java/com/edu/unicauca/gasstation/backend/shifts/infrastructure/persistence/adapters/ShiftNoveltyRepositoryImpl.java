package com.edu.unicauca.gasstation.backend.shifts.infrastructure.persistence.adapters;

import com.edu.unicauca.gasstation.backend.shifts.domain.models.ShiftNovelty;
import com.edu.unicauca.gasstation.backend.shifts.domain.repositories.ShiftNoveltyRepository;
import com.edu.unicauca.gasstation.backend.shifts.infrastructure.persistence.entities.ShiftNoveltyEntity;
import com.edu.unicauca.gasstation.backend.shifts.infrastructure.persistence.entities.ShiftCodeEntity;
import com.edu.unicauca.gasstation.backend.shifts.infrastructure.persistence.mappers.ShiftCodePersistenceMapper;
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
    private final ShiftCodePersistenceMapper shiftCodeMapper;

    @Override
    @Transactional
    public ShiftNovelty save(ShiftNovelty novelty) {
        ShiftNoveltyEntity saved = repository.save(toEntity(novelty,
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
        return repository.findWithCodesById(id).map(this::toDomain);
    }

    @Override
    public List<ShiftNovelty> findByWorkDateBetween(LocalDate from, LocalDate to) {
        return repository.findByWorkDateBetweenAndCancelledAtIsNullOrderByCreatedAtAscIdAsc(from, to).stream()
                .map(this::toDomain)
                .toList();
    }

    /** Both shift codes of the entity and their roles must already be loaded (the JPA queries use an entity graph). */
    private ShiftNovelty toDomain(ShiftNoveltyEntity entity) {
        return ShiftNovelty.restore(entity.getId(), entity.getWorkDate(), entity.getType(), entity.getWorkerId(),
                entity.getCounterpartWorkerId(),
                shiftCodeMapper.toDomain(entity.getOriginalShiftCode()),
                entity.getFinalShiftCode() == null ? null : shiftCodeMapper.toDomain(entity.getFinalShiftCode()),
                entity.isExceptionalCoverage(), entity.getReason(), entity.getCreatedAt(), entity.getCancelledAt());
    }

    /**
     * @param originalShiftCode managed reference to the original shift code row
     * @param finalShiftCode    managed reference to the final shift code row, or null in ABSENCE
     */
    private static ShiftNoveltyEntity toEntity(ShiftNovelty novelty, ShiftCodeEntity originalShiftCode,
                                               ShiftCodeEntity finalShiftCode) {
        ShiftNoveltyEntity entity = new ShiftNoveltyEntity();
        entity.setId(novelty.getId());
        entity.setWorkDate(novelty.getWorkDate());
        entity.setType(novelty.getType());
        entity.setWorkerId(novelty.getWorkerId());
        entity.setCounterpartWorkerId(novelty.getCounterpartWorkerId());
        entity.setOriginalShiftCode(originalShiftCode);
        entity.setFinalShiftCode(finalShiftCode);
        entity.setExceptionalCoverage(novelty.isExceptionalCoverage());
        entity.setReason(novelty.getReason());
        entity.setCreatedAt(novelty.getCreatedAt());
        entity.setCancelledAt(novelty.getCancelledAt());
        return entity;
    }
}
