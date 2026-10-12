package com.edu.unicauca.gasstation.backend.sales.api.dto;

import com.edu.unicauca.gasstation.backend.sales.domain.calculation.DistributionProfile;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.util.*;

public record CreateDistributionProfileRequest(@NotBlank @Size(max=100) String name, @NotBlank @Size(max=500) String source,
        @NotNull @Size(min=24,max=24) List<@NotNull @DecimalMin("0") BigDecimal> hourlyWeights,
        @NotNull @Size(min=7,max=7) Map<DayOfWeek, @NotNull @DecimalMin("0") BigDecimal> historicalDailyGallons,
        @NotNull Set<@NotBlank @Size(max=30) String> supportShiftCodes,
        @NotNull BigDecimal titularShare, @NotNull BigDecimal supportShare,
        @NotNull BigDecimal referenceTitularShare, @NotNull BigDecimal referenceSupportShare) {
    public DistributionProfile toDomain() {
        return new DistributionProfile(name, source, hourlyWeights, historicalDailyGallons, supportShiftCodes,
                titularShare, supportShare, referenceTitularShare, referenceSupportShare);
    }
}
