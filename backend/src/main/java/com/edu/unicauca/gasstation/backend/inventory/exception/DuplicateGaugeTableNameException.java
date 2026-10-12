package com.edu.unicauca.gasstation.backend.inventory.exception;

public class DuplicateGaugeTableNameException extends RuntimeException {
    public DuplicateGaugeTableNameException(String name) {
        super(" Already exists a gauge table with the name: "+name);
    }
}
