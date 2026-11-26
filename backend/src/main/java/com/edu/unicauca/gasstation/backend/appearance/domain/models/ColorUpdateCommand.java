package com.edu.unicauca.gasstation.backend.appearance.domain.models;

/** Single color update inside a batch request; a {@code null} or blank {@code colorHex} resets the key to default. */
public record ColorUpdateCommand(ColorCategory category, String entityKey, String colorHex) {
}
