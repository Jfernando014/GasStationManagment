package com.edu.unicauca.gasstation.backend.shifts.domain.repositories;

import com.edu.unicauca.gasstation.backend.shifts.domain.models.Role;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Port for storing and reading roles. Implemented by {@code infrastructure.persistence.adapters.RoleRepositoryImpl}.
 */
public interface RoleRepository {

    Role save(Role role);

    Optional<Role> findByName(String name);

    /** Roles whose id is in {@code ids}; ids that do not exist are simply not returned. */
    List<Role> findAllByIds(Collection<UUID> ids);
}
