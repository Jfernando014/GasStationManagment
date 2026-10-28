package com.edu.unicauca.gasstation.backend.inventory.domain.models;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

@Getter
@AllArgsConstructor
public class SalesSyncResult {

    private final LocalDate journalDate;
    /** Products whose sales were recorded. */
    private final int syncedProducts;
    /** Sold codes that do not match any product of the catalog. */
    private final List<String> unmatchedCodes;
}