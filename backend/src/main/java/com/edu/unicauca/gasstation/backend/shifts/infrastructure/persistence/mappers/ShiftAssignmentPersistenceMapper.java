package com.edu.unicauca.gasstation.backend.shifts.infrastructure.persistence.mappers;

import com.edu.unicauca.gasstation.backend.shifts.domain.models.ShiftAssignment;
import com.edu.unicauca.gasstation.backend.shifts.infrastructure.persistence.entities.ShiftAssignmentEntity;
import com.edu.unicauca.gasstation.backend.shifts.infrastructure.persistence.entities.ShiftCodeEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Converts between the domain {@link ShiftAssignment} and the JPA {@link ShiftAssignmentEntity}.
 */
@Component
@RequiredArgsConstructor
public class ShiftAssignmentPersistenceMapper {

    private final ShiftCodePersistenceMapper shiftCodePersistenceMapper;

    /** The entity's shift code and its role must already be loaded (the JPA queries use an entity graph). */
    public ShiftAssignment toDomain(ShiftAssignmentEntity entity) {
        return ShiftAssignment.restore(entity.getId(), entity.getWorkerId(), entity.getWorkDate(),
                shiftCodePersistenceMapper.toDomain(entity.getShiftCode()), entity.getNote());
    }

    /**
     * @param shiftCode managed reference to the shift code row. The adapter provides it so the foreign key is
     *                  written without copying the shift code data.
     */
    public ShiftAssignmentEntity toEntity(ShiftAssignment assignment, ShiftCodeEntity shiftCode) {
        ShiftAssignmentEntity entity = new ShiftAssignmentEntity();
        entity.setId(assignment.getId());
        entity.setWorkerId(assignment.getWorkerId());
        entity.setWorkDate(assignment.getWorkDate());
        entity.setShiftCode(shiftCode);
        entity.setNote(assignment.getNote());
        return entity;
    }
}
