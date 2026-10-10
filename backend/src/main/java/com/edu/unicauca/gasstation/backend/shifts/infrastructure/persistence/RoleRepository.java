package com.edu.unicauca.gasstation.backend.shifts.infrastructure.persistence;

import com.edu.unicauca.gasstation.backend.shifts.domain.models.Role;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data repository of {@link Role}.
 */
public interface RoleRepository extends JpaRepository<Role, UUID> {

    Optional<Role> findByName(String name);
}
