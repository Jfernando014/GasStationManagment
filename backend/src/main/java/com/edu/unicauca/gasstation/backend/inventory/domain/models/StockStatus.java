package com.edu.unicauca.gasstation.backend.inventory.domain.models;

public enum StockStatus {
    OK,
    /**
     * The closing stock of some location is below zero (e.g. more units sold than the counter had).
     */
    NEGATIVE_STOCK
}

