package com.edu.unicauca.gasstation.backend.sales.api.dto;

import com.edu.unicauca.gasstation.backend.sales.domain.models.SavingsFundMovement;
import com.edu.unicauca.gasstation.backend.sales.domain.models.SavingsFundMovementType;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record SavingsFundMovementResponseDto(
        Long id,
        SavingsFundMovementType type,
        LocalDate date,
        BigDecimal amount,
        String description,
        Instant createdAt) {

    public static SavingsFundMovementResponseDto from(SavingsFundMovement movement) {
        return new SavingsFundMovementResponseDto(
                movement.getId(),
                movement.getMovementType(),
                movement.getMovementDate(),
                movement.getAmount(),
                movement.getDescription(),
                movement.getCreatedAt());
    }
}
