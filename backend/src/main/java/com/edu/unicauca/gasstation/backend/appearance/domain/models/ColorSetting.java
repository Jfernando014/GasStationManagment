package com.edu.unicauca.gasstation.backend.appearance.domain.models;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Persisted color override for a single entity key inside a category. */
public record ColorSetting(
        UUID id,
        ColorCategory category,
        String entityKey,
        ColorHex color,
        Instant updatedAt) {

    public ColorSetting {
        Objects.requireNonNull(category, "category");
        Objects.requireNonNull(entityKey, "entityKey");
        Objects.requireNonNull(color, "color");
        Objects.requireNonNull(updatedAt, "updatedAt");
    }

    public static ColorSetting create(ColorCategory category, String entityKey, ColorHex color, Instant now) {
        return new ColorSetting(null, category, entityKey, color, now);
    }

    public ColorSetting withColor(ColorHex newColor, Instant now) {
        return new ColorSetting(id, category, entityKey, newColor, now);
    }
}
