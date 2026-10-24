package com.edu.unicauca.gasstation.backend.shifts.infrastructure.persistence.mappers;

import com.edu.unicauca.gasstation.backend.shifts.domain.models.ShiftNovelty;
import com.edu.unicauca.gasstation.backend.shifts.infrastructure.persistence.entities.ShiftCodeEntity;
import com.edu.unicauca.gasstation.backend.shifts.infrastructure.persistence.entities.ShiftNoveltyEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Converts between the domain {@link ShiftNovelty} and the JPA {@link ShiftNoveltyEntity}.
 */
@Component
@RequiredArgsConstructor
public class ShiftNoveltyPersistenceMapper {

    private final ShiftCodePersistenceMapper shiftCodePersistenceMapper;

    /** Both shift codes of the entity and their roles must already be loaded (the JPA queries use an entity graph). */
    public ShiftNovelty toDomain(ShiftNoveltyEntity entity) {
        return ShiftNovelty.restore(entity.getId(), entity.getWorkDate(), entity.getType(), entity.getWorkerId(),
                entity.getCounterpartWorkerId(),
                shiftCodePersistenceMapper.toDomain(entity.getOriginalShiftCode()),
                entity.getFinalShiftCode() == null ? null : shiftCodePersistenceMapper.toDomain(entity.getFinalShiftCode()),
                entity.isExceptionalCoverage(), entity.getReason(), entity.getCreatedAt(), entity.getCancelledAt());
    }

    /**
     * @param originalShiftCode managed reference to the original shift code row
     * @param finalShiftCode    managed reference to the final shift code row, or null in ABSENCE
     */
    public ShiftNoveltyEntity toEntity(ShiftNovelty novelty, ShiftCodeEntity originalShiftCode,
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
