package com.edu.unicauca.gasstation.backend.inventory.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class SalesSyncResponse {

    private LocalDate journalDate;

    @Schema(description = "Products whose sales were recorded")
    private int syncedProducts;

    @Schema(description = "Sold codes that do not match any product of the catalog")
    private List<String> unmatchedCodes;
}