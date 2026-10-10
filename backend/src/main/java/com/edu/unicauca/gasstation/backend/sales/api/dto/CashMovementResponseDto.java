package com.edu.unicauca.gasstation.backend.sales.api.dto;

import com.edu.unicauca.gasstation.backend.sales.domain.models.CashMovement;
import com.edu.unicauca.gasstation.backend.sales.domain.models.CashMovementType;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record CashMovementResponseDto(
        Long id,
        CashMovementType type,
        LocalDate date,
        BigDecimal amount,
        String description,
        Instant createdAt) {

    public static CashMovementResponseDto from(CashMovement movement) {
        return new CashMovementResponseDto(
                movement.getId(),
                movement.getMovementType(),
                movement.getMovementDate(),
                movement.getAmount(),
                movement.getDescription(),
                movement.getCreatedAt());
    }
}
