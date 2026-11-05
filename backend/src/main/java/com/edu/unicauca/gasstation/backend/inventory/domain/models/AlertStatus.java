package com.edu.unicauca.gasstation.backend.inventory.domain.models;

public enum AlertStatus {
    ACTIVE,     // detected, nobody has seen it
    READ,       // the administrator opened it
    ATTENDED,   // the administrator acted on it (with a note); the condition may still hold
    RESOLVED;   // the condition is over, closed automatically

    public boolean isOpen() {
        return this != RESOLVED;
    }
}