package com.edu.unicauca.gasstation.backend.shifts.application;

import com.edu.unicauca.gasstation.backend.shifts.ShiftExternalService;
import com.edu.unicauca.gasstation.backend.shifts.api.dtos.ShiftCodeResponse;
import com.edu.unicauca.gasstation.backend.shifts.domain.ShiftCode;
import com.edu.unicauca.gasstation.backend.shifts.infrastructure.persistence.ShiftCodeRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
class ShiftExternalServiceImpl implements ShiftExternalService {

    private final ShiftCatalogService shiftCatalogService;
    private final ShiftCodeRepository shiftCodeRepository;

    @Override
    public Optional<UUID> findShiftCodeIdByCode(String code) {
        return shiftCodeRepository.findByCode(code).map(ShiftCode::getId);
    }

    @Override
    public List<ShiftCodeResponse> getActiveCatalog() {
        return shiftCatalogService.listActive();
    }
}
