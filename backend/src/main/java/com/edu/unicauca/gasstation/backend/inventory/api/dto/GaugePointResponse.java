package com.edu.unicauca.gasstation.backend.inventory.api.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
public class GaugePointResponse {

    private BigDecimal heightCm;
    private BigDecimal volumeGallons;
}