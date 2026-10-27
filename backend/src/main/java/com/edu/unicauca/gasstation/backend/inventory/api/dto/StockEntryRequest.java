package com.edu.unicauca.gasstation.backend.inventory.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "Units of a product that arrive at the office")
public class StockEntryRequest {

    public static final String PRODUCT_REQUIRED = "The product is required";
    public static final String DATE_REQUIRED = "The journal date is required";
    public static final String QUANTITY_REQUIRED = "The quantity is required";
    public static final String QUANTITY_POSITIVE = "The quantity must be greater than zero";
    public static final String QUANTITY_DIGITS = "The quantity must be a whole number of up to 6 digits";
    public static final String NOTE_SIZE = "The note cannot exceed 200 characters";

    @NotNull(message = PRODUCT_REQUIRED)
    private Long productId;

    @NotNull(message = QUANTITY_REQUIRED)
    @Positive(message = QUANTITY_POSITIVE)
    @Digits(integer = 6, fraction = 0, message = QUANTITY_DIGITS)
    private BigDecimal quantity;

    @NotNull(message = DATE_REQUIRED)
    @Schema(example = "2026-08-05")
    private LocalDate journalDate;

    @Size(max = 200, message = NOTE_SIZE)
    @Schema(description = "Optional note, for example the supplier or the invoice")
    private String note;
}