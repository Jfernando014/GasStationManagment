package com.edu.unicauca.gasstation.backend.shifts.infrastructure.persistence.adapters;

import com.edu.unicauca.gasstation.backend.shifts.domain.models.ShiftCode;
import com.edu.unicauca.gasstation.backend.shifts.domain.repositories.ShiftCodeRepository;
import com.edu.unicauca.gasstation.backend.shifts.infrastructure.persistence.entities.RoleEntity;
import com.edu.unicauca.gasstation.backend.shifts.infrastructure.persistence.mappers.ShiftCodePersistenceMapper;
import com.edu.unicauca.gasstation.backend.shifts.infrastructure.persistence.repositories.JpaRoleRepository;
import com.edu.unicauca.gasstation.backend.shifts.infrastructure.persistence.repositories.JpaShiftCodeRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implements the domain {@link ShiftCodeRepository} with Spring Data JPA.
 */
@Repository
@RequiredArgsConstructor
public class ShiftCodeRepositoryImpl implements ShiftCodeRepository {

    private final JpaShiftCodeRepository repository;
    private final JpaRoleRepository roleRepository;
    private final ShiftCodePersistenceMapper mapper;

    /** Transactional so the role reference can still be loaded when mapping the saved entity back. */
    @Override
    @Transactional
    public ShiftCode save(ShiftCode shiftCode) {
        // A reference (proxy) is enough to write role_id; the role row is not modified
        RoleEntity role = shiftCode.getRole() == null
                ? null
                : roleRepository.getReferenceById(shiftCode.getRole().getId());
        return mapper.toDomain(repository.save(mapper.toEntity(shiftCode, role)));
    }

    @Override
    public Optional<ShiftCode> findByCode(String code) {
        return repository.findByCode(code).map(mapper::toDomain);
    }

    @Override
    public List<ShiftCode> findActive() {
        return repository.findByActiveTrue().stream().map(mapper::toDomain).toList();
    }
}
