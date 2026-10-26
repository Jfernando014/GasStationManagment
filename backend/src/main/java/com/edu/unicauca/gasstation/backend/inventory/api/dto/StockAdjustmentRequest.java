package com.edu.unicauca.gasstation.backend.inventory.api.dto;

import com.edu.unicauca.gasstation.backend.inventory.domain.models.StockLocation;
import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "Correction of the stock of a product in a location")
public class StockAdjustmentRequest {

    public static final String PRODUCT_REQUIRED = "The product is required";
    public static final String LOCATION_REQUIRED = "The location is required";
    public static final String DATE_REQUIRED = "The journal date is required";
    public static final String QUANTITY_REQUIRED = "The quantity is required";
    public static final String QUANTITY_DIGITS = "The quantity must be a whole number of up to 6 digits";
    public static final String REASON_REQUIRED = "The reason of the adjustment is required";
    public static final String REASON_SIZE = "The reason cannot exceed 200 characters";

    @NotNull(message = PRODUCT_REQUIRED)
    private Long productId;

    @NotNull(message = LOCATION_REQUIRED)
    private StockLocation location;

    @NotNull(message = QUANTITY_REQUIRED)
    @Digits(integer = 6, fraction = 0, message = QUANTITY_DIGITS)
    @Schema(description = "Signed units: positive adds, negative removes. Cannot be zero", example = "-2")
    private BigDecimal quantity;

    @NotNull(message = DATE_REQUIRED)
    @Schema(example = "2026-08-05")
    private LocalDate journalDate;

    @NotBlank(message = REASON_REQUIRED)
    @Size(max = 200, message = REASON_SIZE)
    private String reason;
}