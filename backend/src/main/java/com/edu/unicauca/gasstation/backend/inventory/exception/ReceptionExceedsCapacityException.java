package com.edu.unicauca.gasstation.backend.inventory.exception;

public class ReceptionExceedsCapacityException extends RuntimeException {

    public ReceptionExceedsCapacityException() {
        super("The quantity exceeds the available capacity of the tank");
    }
}