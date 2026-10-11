package com.edu.unicauca.gasstation.backend.inventory.api.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GaugeVolumeResponse {
    private Long tankId;
    private BigDecimal heightCm;
    private BigDecimal volumeGallons;
}