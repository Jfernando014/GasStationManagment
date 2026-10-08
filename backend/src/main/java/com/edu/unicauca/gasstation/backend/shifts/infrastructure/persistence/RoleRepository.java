package com.edu.unicauca.gasstation.backend.shifts.infrastructure.persistence;

import com.edu.unicauca.gasstation.backend.shifts.domain.Role;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<Role, UUID> {

    Optional<Role> findByName(String name);
}
