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
@Schema(description = "Inventory control of a journal")
public class DailyControlResponse {

    private LocalDate journalDate;
    private List<FuelControlResponse> fuels;
    private List<ProductControlResponse> products;
}