package com.edu.unicauca.gasstation.backend.inventory.api.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class TankSummaryResponse {

    private Long id;
    private String code;
    private String name;
}