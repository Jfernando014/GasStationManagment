package com.edu.unicauca.gasstation.backend.shifts.domain.services;

import com.edu.unicauca.gasstation.backend.shifts.RoleInfo;
import com.edu.unicauca.gasstation.backend.shifts.ShiftCodeInfo;
import com.edu.unicauca.gasstation.backend.shifts.ShiftExternalService;
import com.edu.unicauca.gasstation.backend.shifts.domain.models.Role;
import com.edu.unicauca.gasstation.backend.shifts.domain.models.ShiftCode;
import com.edu.unicauca.gasstation.backend.shifts.exception.ShiftCodeNotFoundException;
import com.edu.unicauca.gasstation.backend.shifts.infrastructure.persistence.RoleRepository;
import com.edu.unicauca.gasstation.backend.shifts.infrastructure.persistence.ShiftCodeRepository;
import java.util.ArrayList;
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
 * Read-only use cases of the shift catalog (S1-22).
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
        return shiftCodeRepository.findByActiveTrue();
    }

    /**
     * One shift code by its code (DIA6, 12-7...), active or not.
     *
     * @throws ShiftCodeNotFoundException if the code does not exist
     */
    public ShiftCode getByCode(String code) {
        return shiftCodeRepository.findByCode(code).orElseThrow(() -> new ShiftCodeNotFoundException(code));
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
        if (roleIds.isEmpty()) {
            return Map.of();
        }
        return roleRepository.findAllById(roleIds).stream()
                .map(ShiftCatalogService::toInfo)
                .collect(Collectors.toMap(RoleInfo::id, roleInfo -> roleInfo));
    }

    private static RoleInfo toInfo(Role role) {
        return new RoleInfo(role.getId(), role.getName(), role.getDispenser());
    }

    private static ShiftCodeInfo toInfo(ShiftCode shiftCode) {
        List<ShiftCodeInfo.Segment> segments = new ArrayList<>();
        if (shiftCode.getStartHour1() != null) {
            segments.add(new ShiftCodeInfo.Segment(shiftCode.getStartHour1(), shiftCode.getEndHour1()));
        }
        if (shiftCode.getStartHour2() != null) {
            segments.add(new ShiftCodeInfo.Segment(shiftCode.getStartHour2(), shiftCode.getEndHour2()));
        }
        return new ShiftCodeInfo(
                shiftCode.getId(),
                shiftCode.getCode(),
                shiftCode.getName(),
                shiftCode.getRole() == null ? null : toInfo(shiftCode.getRole()),
                shiftCode.getPeriod() == null ? null : shiftCode.getPeriod().name(),
                segments,
                shiftCode.totalHours(),
                shiftCode.isRequiresSupport(),
                shiftCode.getPairedWithId(),
                shiftCode.isActive());
    }
}
