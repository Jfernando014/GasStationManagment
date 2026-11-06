package com.edu.unicauca.gasstation.backend.inventory.exception;

public class InventoryAlertNotFoundException extends RuntimeException {

    public InventoryAlertNotFoundException(Long id) {
        super("Inventory alert not found with id " + id);
    }
}