package com.edu.unicauca.gasstation.backend.shifts.infrastructure.persistence.mappers;

import com.edu.unicauca.gasstation.backend.shifts.domain.models.ShiftCode;
import com.edu.unicauca.gasstation.backend.shifts.infrastructure.persistence.entities.RoleEntity;
import com.edu.unicauca.gasstation.backend.shifts.infrastructure.persistence.entities.ShiftCodeEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Converts between the domain {@link ShiftCode} and the JPA {@link ShiftCodeEntity}.
 */
@Component
@RequiredArgsConstructor
public class ShiftCodePersistenceMapper {

    private final RolePersistenceMapper rolePersistenceMapper;

    /** The entity's role must already be loaded (the JPA queries use an entity graph). */
    public ShiftCode toDomain(ShiftCodeEntity entity) {
        return ShiftCode.builder()
                .id(entity.getId())
                .code(entity.getCode())
                .name(entity.getName())
                .role(entity.getRole() == null ? null : rolePersistenceMapper.toDomain(entity.getRole()))
                .period(entity.getPeriod())
                .startHour1(entity.getStartHour1())
                .endHour1(entity.getEndHour1())
                .startHour2(entity.getStartHour2())
                .endHour2(entity.getEndHour2())
                .requiresSupport(entity.isRequiresSupport())
                .pairedWithId(entity.getPairedWithId())
                .active(entity.isActive())
                .build();
    }

    /**
     * @param role managed reference to the role row, or null for DESCANSO. The adapter provides it so the
     *             foreign key is written without copying the role data.
     */
    public ShiftCodeEntity toEntity(ShiftCode shiftCode, RoleEntity role) {
        ShiftCodeEntity entity = new ShiftCodeEntity();
        entity.setId(shiftCode.getId());
        entity.setCode(shiftCode.getCode());
        entity.setName(shiftCode.getName());
        entity.setRole(role);
        entity.setPeriod(shiftCode.getPeriod());
        entity.setStartHour1(shiftCode.getStartHour1());
        entity.setEndHour1(shiftCode.getEndHour1());
        entity.setStartHour2(shiftCode.getStartHour2());
        entity.setEndHour2(shiftCode.getEndHour2());
        entity.setRequiresSupport(shiftCode.isRequiresSupport());
        entity.setPairedWithId(shiftCode.getPairedWithId());
        entity.setActive(shiftCode.isActive());
        return entity;
    }
}
