package com.edu.unicauca.gasstation.backend.inventory.domain.models;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
public class DailyControl {

    private final LocalDate journalDate;
    private final List<FuelControl> fuels;
    private final List<ProductControl> products;
}