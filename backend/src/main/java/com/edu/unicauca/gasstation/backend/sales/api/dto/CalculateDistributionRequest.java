package com.edu.unicauca.gasstation.backend.sales.api.dto;

import com.edu.unicauca.gasstation.backend.sales.domain.calculation.DistributionCalculation.Assignment;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.util.*;

/** Explicit schedule input; never interpreted as assignments fetched from shifts. */
public record CalculateDistributionRequest(@NotNull Long profileId, @NotNull @Min(0) @Max(12) Integer outputScale,
        @NotNull @Size(min=1) List<@NotNull @Valid AssignmentRequest> assignments) {
    public record AssignmentRequest(@NotNull LocalDate date, @NotNull UUID workerId, @NotBlank String shiftCode) {
        public Assignment toDomain() { return new Assignment(date, workerId, shiftCode); }
    }
}
