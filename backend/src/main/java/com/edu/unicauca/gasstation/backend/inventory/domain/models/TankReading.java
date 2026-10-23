package com.edu.unicauca.gasstation.backend.inventory.domain.models;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/** Lectura de un tanque: la altura medida con la regla y los galones calculados con su tabla de aforo. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TankReading {

    private Long tankId;
    private BigDecimal heightCm;
    private BigDecimal volumeGallons;
}