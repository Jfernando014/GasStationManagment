package com.edu.unicauca.gasstation.backend.shifts.infrastructure.persistence.repositories;

import com.edu.unicauca.gasstation.backend.shifts.infrastructure.persistence.entities.RoleEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data repository of {@link RoleEntity}. Used only by {@code RoleRepositoryImpl}.
 */
public interface JpaRoleRepository extends JpaRepository<RoleEntity, UUID> {

    Optional<RoleEntity> findByName(String name);
}
