package com.edu.unicauca.gasstation.backend.shifts.domain.services;

import com.edu.unicauca.gasstation.backend.shifts.catalog.RoleInfo;
import com.edu.unicauca.gasstation.backend.shifts.catalog.ShiftCodeInfo;
import com.edu.unicauca.gasstation.backend.shifts.catalog.ShiftExternalService;
import com.edu.unicauca.gasstation.backend.shifts.domain.models.ShiftCode;
import com.edu.unicauca.gasstation.backend.shifts.domain.repositories.RoleRepository;
import com.edu.unicauca.gasstation.backend.shifts.domain.repositories.ShiftCodeRepository;
import com.edu.unicauca.gasstation.backend.shifts.exception.ShiftError;
import com.edu.unicauca.gasstation.backend.shifts.exception.ShiftException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Read-only use cases of the shift catalog.
 *
 * <p>Serves two clients: the REST API of this module (returns domain models, mapped to DTOs in the
 * controller) and other modules through {@link ShiftExternalService} (returns the public {@code *Info} records).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ShiftCatalogService implements ShiftExternalService {

    private final ShiftCodeRepository shiftCodeRepository;
    private final RoleRepository roleRepository;

    /** Every active shift code, with its role. */
    public List<ShiftCode> listActive() {
        return shiftCodeRepository.findActive();
    }

    /**
     * One shift code by its code (DIA6, 12-7...), active or not.
     *
     * @throws ShiftException {@code SHIFT_CODE_NOT_FOUND} if the code does not exist
     */
    public ShiftCode getByCode(String code) {
        return shiftCodeRepository.findByCode(code).orElseThrow(() -> new ShiftException(ShiftError.SHIFT_CODE_NOT_FOUND, code));
    }

    @Override
    public Optional<UUID> findShiftCodeIdByCode(String code) {
        return shiftCodeRepository.findByCode(code).map(ShiftCode::getId);
    }

    @Override
    public List<ShiftCodeInfo> getActiveCatalog() {
        return listActive().stream().map(ShiftCatalogService::toInfo).toList();
    }

    @Override
    public Map<UUID, RoleInfo> getRolesByIds(Collection<UUID> roleIds) {
        return roleRepository.findAllByIds(roleIds).stream()
                .map(ShiftInfoMapper::toRoleInfo)
                .collect(Collectors.toMap(RoleInfo::id, roleInfo -> roleInfo));
    }

    private static ShiftCodeInfo toInfo(ShiftCode shiftCode) {
        return new ShiftCodeInfo(
                shiftCode.getId(),
                shiftCode.getCode(),
                shiftCode.getName(),
                shiftCode.getRole() == null ? null : ShiftInfoMapper.toRoleInfo(shiftCode.getRole()),
                shiftCode.getPeriod() == null ? null : shiftCode.getPeriod().name(),
                ShiftInfoMapper.toSegments(shiftCode),
                shiftCode.totalHours(),
                shiftCode.isRequiresSupport(),
                shiftCode.getPairedWithId(),
                shiftCode.isActive());
    }
}
