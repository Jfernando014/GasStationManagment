package com.edu.unicauca.gasstation.backend.sales.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Tolerance configuration overview containing active version, scheduled versions and history")
public record DiscrepancyToleranceOverviewDto(
        @Schema(description = "Currently active version for the queried date")
        DiscrepancyToleranceResponseDto active,

        @Schema(description = "Future scheduled tolerance versions")
        List<DiscrepancyToleranceResponseDto> scheduled,

        @Schema(description = "Complete tolerance change history")
        List<DiscrepancyToleranceResponseDto> history
) {}
