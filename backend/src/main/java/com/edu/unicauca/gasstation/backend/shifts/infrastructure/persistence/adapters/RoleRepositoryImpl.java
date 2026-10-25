package com.edu.unicauca.gasstation.backend.shifts.infrastructure.persistence.adapters;

import com.edu.unicauca.gasstation.backend.shifts.domain.models.Role;
import com.edu.unicauca.gasstation.backend.shifts.domain.repositories.RoleRepository;
import com.edu.unicauca.gasstation.backend.shifts.infrastructure.persistence.entities.RoleEntity;
import com.edu.unicauca.gasstation.backend.shifts.infrastructure.persistence.repositories.JpaRoleRepository;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/**
 * Implements the domain {@link RoleRepository} with Spring Data JPA.
 */
@Repository
@RequiredArgsConstructor
public class RoleRepositoryImpl implements RoleRepository {

    private final JpaRoleRepository repository;

    @Override
    public Role save(Role role) {
        return toDomain(repository.save(toEntity(role)));
    }

    @Override
    public Optional<Role> findByName(String name) {
        return repository.findByName(name).map(RoleRepositoryImpl::toDomain);
    }

    @Override
    public List<Role> findAllByIds(Collection<UUID> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }
        return repository.findAllById(ids).stream().map(RoleRepositoryImpl::toDomain).toList();
    }

    private static Role toDomain(RoleEntity entity) {
        return new Role(entity.getId(), entity.getName(), entity.getDispenser());
    }

    private static RoleEntity toEntity(Role role) {
        RoleEntity entity = new RoleEntity();
        entity.setId(role.getId());
        entity.setName(role.getName());
        entity.setDispenser(role.getDispenser());
        return entity;
    }
}
