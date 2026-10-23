package com.edu.unicauca.gasstation.backend.inventory.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
public class FuelReceptionRequest {

    public static final String TANK_REQUIRED = "The tank is required";
    public static final String DATE_INVALID = "The reception date is not valid";
    public static final String GALLONS_REQUIRED = "The quantity is required";
    public static final String GALLONS_POSITIVE = "The quantity must be a numeric value greater than zero";
    public static final String GALLONS_DIGITS = "The quantity must have at most 6 integer digits and 2 decimals";
    public static final String SUPPLIER_REQUIRED = "The supplier is required";
    public static final String SUPPLIER_SIZE = "The supplier cannot exceed 100 characters";

    @Schema(description = "Tank that receives the fuel", example = "1")
    @NotNull(message = TANK_REQUIRED)
    private Long tankId;

    @Schema(description = "Date of the reception (not in the future)", example = "2026-08-05")
    @NotNull(message = DATE_INVALID)
    private LocalDate receptionDate;

    @Schema(description = "Received gallons", example = "3970")
    @NotNull(message = GALLONS_REQUIRED)
    @DecimalMin(value = "0", inclusive = false, message = GALLONS_POSITIVE)
    @Digits(integer = 6, fraction = 2, message = GALLONS_DIGITS)
    private BigDecimal gallons;

    @Schema(description = "Supplier of the fuel", example = "Terpel")
    @NotBlank(message = SUPPLIER_REQUIRED)
    @Size(max = 100, message = SUPPLIER_SIZE)
    private String supplier;
}