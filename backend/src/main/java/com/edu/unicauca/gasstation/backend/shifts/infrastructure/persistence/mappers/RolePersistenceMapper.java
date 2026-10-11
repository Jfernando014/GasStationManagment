package com.edu.unicauca.gasstation.backend.shifts.infrastructure.persistence.mappers;

import com.edu.unicauca.gasstation.backend.shifts.domain.models.Role;
import com.edu.unicauca.gasstation.backend.shifts.infrastructure.persistence.entities.RoleEntity;
import org.springframework.stereotype.Component;

/**
 * Converts between the domain {@link Role} and the JPA {@link RoleEntity}.
 */
@Component
public class RolePersistenceMapper {

    public Role toDomain(RoleEntity entity) {
        return new Role(entity.getId(), entity.getName(), entity.getDispenser());
    }

    public RoleEntity toEntity(Role role) {
        RoleEntity entity = new RoleEntity();
        entity.setId(role.getId());
        entity.setName(role.getName());
        entity.setDispenser(role.getDispenser());
        return entity;
    }
}
