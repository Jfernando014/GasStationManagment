package com.edu.unicauca.gasstation.backend.inventory.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "Opening stock of a product in the office and in the counter")
public class InitialStockRequest {

    public static final String PRODUCT_REQUIRED = "The product is required";
    public static final String DATE_REQUIRED = "The journal date is required";
    public static final String INITIAL_INVALID =
            "The initial stock must be a whole number greater than or equal to zero";

    @NotNull(message = PRODUCT_REQUIRED)
    private Long productId;

    @NotNull(message = INITIAL_INVALID)
    @PositiveOrZero(message = INITIAL_INVALID)
    @Digits(integer = 6, fraction = 0, message = INITIAL_INVALID)
    @Schema(description = "Units in the office", example = "12")
    private BigDecimal officeQuantity;

    @NotNull(message = INITIAL_INVALID)
    @PositiveOrZero(message = INITIAL_INVALID)
    @Digits(integer = 6, fraction = 0, message = INITIAL_INVALID)
    @Schema(description = "Units in the counter", example = "5")
    private BigDecimal counterQuantity;

    @NotNull(message = DATE_REQUIRED)
    @Schema(description = "Date the opening stock applies from", example = "2026-08-01")
    private LocalDate journalDate;
}