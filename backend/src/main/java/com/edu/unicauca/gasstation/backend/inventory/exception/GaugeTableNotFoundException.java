package com.edu.unicauca.gasstation.backend.inventory.exception;

public class GaugeTableNotFoundException extends RuntimeException {
    public GaugeTableNotFoundException(Long id) {
        super(" The gauge table with ID could not be found" + id);
    }
}
