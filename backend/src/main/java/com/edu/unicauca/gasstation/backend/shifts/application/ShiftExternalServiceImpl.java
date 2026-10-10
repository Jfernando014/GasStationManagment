package com.edu.unicauca.gasstation.backend.shifts.application;

import com.edu.unicauca.gasstation.backend.shifts.RoleInfo;
import com.edu.unicauca.gasstation.backend.shifts.ShiftExternalService;
import com.edu.unicauca.gasstation.backend.shifts.api.dtos.ShiftCodeResponse;
import com.edu.unicauca.gasstation.backend.shifts.domain.ShiftCode;
import com.edu.unicauca.gasstation.backend.shifts.infrastructure.persistence.RoleRepository;
import com.edu.unicauca.gasstation.backend.shifts.infrastructure.persistence.ShiftCodeRepository;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
class ShiftExternalServiceImpl implements ShiftExternalService {

    private final ShiftCatalogService shiftCatalogService;
    private final ShiftCodeRepository shiftCodeRepository;
    private final RoleRepository roleRepository;

    @Override
    public Optional<UUID> findShiftCodeIdByCode(String code) {
        return shiftCodeRepository.findByCode(code).map(ShiftCode::getId);
    }

    @Override
    public List<ShiftCodeResponse> getActiveCatalog() {
        return shiftCatalogService.listActive();
    }

    @Override
    public Map<UUID, RoleInfo> getRolesByIds(Collection<UUID> roleIds) {
        if (roleIds.isEmpty()) {
            return Map.of();
        }
        return roleRepository.findAllById(roleIds).stream()
                .map(role -> new RoleInfo(role.getId(), role.getName(), role.getDispenser()))
                .collect(Collectors.toMap(RoleInfo::id, roleInfo -> roleInfo));
    }
}
