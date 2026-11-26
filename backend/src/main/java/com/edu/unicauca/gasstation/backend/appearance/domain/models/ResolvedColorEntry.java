package com.edu.unicauca.gasstation.backend.appearance.domain.models;

/** Resolved color entry combining system defaults with any persisted override. */
public record ResolvedColorEntry(
        String key,
        String label,
        String colorHex,
        String defaultHex,
        boolean customized) {
}
