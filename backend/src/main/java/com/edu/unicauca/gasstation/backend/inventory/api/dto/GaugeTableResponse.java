package com.edu.unicauca.gasstation.backend.inventory.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class GaugeTableResponse {

    private Long id;
    private String name;
    private List<GaugePointResponse> points;
    private BigDecimal minHeightCm;
    private BigDecimal maxHeightCm;
    private BigDecimal maxVolumeGallons;
    private List<TankSummaryResponse> tanks;
}