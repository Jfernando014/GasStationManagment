package com.edu.unicauca.gasstation.backend.inventory.api.dto;

import com.edu.unicauca.gasstation.backend.inventory.ProductCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
public class ProductRequest {

    public static final String CODE_REQUIRED = "The product code is required";
    public static final String CODE_SIZE = "The code cannot exceed 20 characters";
    public static final String NAME_REQUIRED = "The product name is required";
    public static final String NAME_SIZE = "The name cannot exceed 100 characters";
    public static final String CATEGORY_REQUIRED = "The category is required";
    public static final String PRICE_REQUIRED = "The price is required";
    public static final String PRICE_POSITIVE = "The price must be greater than 0";
    public static final String COST_NOT_NEGATIVE = "The cost cannot be negative";
    public static final String AMOUNT_FORMAT = "Maximum 10 integers and 2 decimal places";
    public static final String ACTIVE_REQUIRED = "You must indicate whether the product is active";


    @Schema(example = "80216")
    @NotBlank(message = CODE_REQUIRED)
    @Size(max = 20, message = CODE_SIZE)
    private String code;

    @Schema(example = "Terpel Celerity 4T Cuarto")
    @NotBlank(message = NAME_REQUIRED)
    @Size(max = 100, message = NAME_SIZE)
    private String name;

    @Schema(example = "LUBRICANTE")
    @NotNull(message = CATEGORY_REQUIRED)
    private ProductCategory category;

    @Schema(example = "28500")
    @NotNull(message = PRICE_REQUIRED)
    @DecimalMin(value = "0.0", inclusive = false, message = PRICE_POSITIVE)
    @Digits(integer = 10, fraction = 2, message = AMOUNT_FORMAT)
    private BigDecimal price;

    @Schema(example = "21924", nullable = true)
    @DecimalMin(value = "0.0", message = COST_NOT_NEGATIVE)
    @Digits(integer = 10, fraction = 2, message = AMOUNT_FORMAT)
    private BigDecimal cost;

    @Schema(example = "true")
    @NotNull(message = ACTIVE_REQUIRED)
    private Boolean active;
}