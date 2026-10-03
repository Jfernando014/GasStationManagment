package com.edu.unicauca.gasstation.backend.inventory.exception;

/**
 * Example custom domain exception for inventory module.
 */
public class InventoryNotFoundExceptionExample extends RuntimeException {

    public InventoryNotFoundExceptionExample(String message) {
        super(message);
    }

    public InventoryNotFoundExceptionExample(Long id) {
        super("Inventory with ID " + id + " not found");
    }
}
