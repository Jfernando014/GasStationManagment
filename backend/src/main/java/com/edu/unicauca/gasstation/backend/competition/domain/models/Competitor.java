package com.edu.unicauca.gasstation.backend.competition.domain.models;

public record Competitor(Long id, String name, String brand, String location, boolean active) {
    public Competitor {
        name = text(name, 150, "name");
        brand = text(brand, 100, "brand");
        location = text(location, 255, "location");
    }

    private static String text(String value, int max, String field) {
        if (value == null || value.isBlank() || value.strip().length() > max)
            throw new IllegalArgumentException("Invalid " + field);
        return value.strip();
    }
}
