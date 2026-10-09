package com.edu.unicauca.gasstation.backend.inventory.api.dto;

import com.edu.unicauca.gasstation.backend.inventory.domain.models.FuelType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TankRequest {
    private static final String REQUIRED_MESSAGE = "The NAME and CAPACITY fields are required";
    private static final String CAPACITY_MESSAGE = "The capacity must be a positive numerical value";
    private static final String MAX_HEIGHT_MESSAGE = "The maximum height must be a positive numerical value";
    private static final String TOLERANCE_MESSAGE = "The tolerance must be a numeric value greater than or equal to zero";

    @Schema(example = "T1C1")
    @NotBlank(message = REQUIRED_MESSAGE)
    @Size(max = 10, message = "The identifier cannot exceed 10 characters")
    private String code;

    @Schema(example = "Tanque 1 Comp. 1")
    @NotBlank(message = REQUIRED_MESSAGE)
    @Size(max = 80, message = "The name cannot exceed 80 characters")
    private String name;

    @Schema(example = "MOTOR")
    @NotNull(message = "The fuel type is required")
    private FuelType fuelType;

    @Schema(example = "3090")
    @NotNull(message = REQUIRED_MESSAGE)
    @Positive(message = CAPACITY_MESSAGE)
    @Digits(integer = 8, fraction = 2, message = CAPACITY_MESSAGE)
    private BigDecimal maxCapacityGallons;

    @Schema(example = "238")
    @NotNull(message = MAX_HEIGHT_MESSAGE)
    @Positive(message = MAX_HEIGHT_MESSAGE)
    @Digits(integer = 4, fraction = 2, message = MAX_HEIGHT_MESSAGE)
    private BigDecimal maxHeightCm;

    @Schema(example = "1")
    @NotNull(message = TOLERANCE_MESSAGE)
    @PositiveOrZero(message = TOLERANCE_MESSAGE)
    @Digits(integer = 3, fraction = 2, message = TOLERANCE_MESSAGE)
    private BigDecimal toleranceCm;


}
