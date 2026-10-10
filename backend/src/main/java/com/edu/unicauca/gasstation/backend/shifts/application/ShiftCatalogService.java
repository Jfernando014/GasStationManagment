package com.edu.unicauca.gasstation.backend.shifts.application;

import com.edu.unicauca.gasstation.backend.shifts.api.dtos.ShiftCodeResponse;
import com.edu.unicauca.gasstation.backend.shifts.exception.ShiftCodeNotFoundException;
import com.edu.unicauca.gasstation.backend.shifts.infrastructure.persistence.ShiftCodeRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ShiftCatalogService {

    private final ShiftCodeRepository shiftCodeRepository;
    private final ShiftCodeMapper shiftCodeMapper;

    public List<ShiftCodeResponse> listActive() {
        return shiftCodeRepository.findByActiveTrue().stream()
                .map(shiftCodeMapper::toResponse)
                .toList();
    }

    public ShiftCodeResponse getByCode(String code) {
        return shiftCodeRepository.findByCode(code)
                .map(shiftCodeMapper::toResponse)
                .orElseThrow(() -> new ShiftCodeNotFoundException(code));
    }
}
