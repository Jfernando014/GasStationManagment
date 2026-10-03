package com.edu.unicauca.gasstation.backend.sales.domain.models;

import java.time.LocalDateTime;

/**
 * Example domain model for sales module.
 * Uses lowerCamelCase for fields and methods.
 */
public class SaleModelExample {

    private Long id;
    private String name;
    private LocalDateTime createdAt;
    private boolean active;

    public SaleModelExample() {
    }

    public SaleModelExample(Long id, String name, LocalDateTime createdAt, boolean active) {
        this.id = id;
        this.name = name;
        this.createdAt = createdAt;
        this.active = active;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
