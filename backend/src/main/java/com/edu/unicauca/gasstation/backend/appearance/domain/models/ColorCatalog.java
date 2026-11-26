package com.edu.unicauca.gasstation.backend.appearance.domain.models;

import com.edu.unicauca.gasstation.backend.appearance.exception.InvalidColorKeyException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Canonical catalog of editable color entries (fuels and shift families), their system defaults, and the curated
 * accessible palette shared with the frontend.
 */
public final class ColorCatalog {

    public record DefaultEntry(String key, String label, String defaultHex) {
    }

    private static final List<DefaultEntry> FUEL_DEFAULTS = List.of(
            new DefaultEntry("MOTOR", "Corriente", "#0284C7"),
            new DefaultEntry("EXTRA", "Extra", "#6366F1"),
            new DefaultEntry("DIESEL", "Diésel", "#10B981"),
            new DefaultEntry("MAX_PRO", "Maxpro", "#F59E0B")
    );

    private static final List<DefaultEntry> SHIFT_DEFAULTS = List.of(
            new DefaultEntry("DIA", "Día", "#FF6B00"),
            new DefaultEntry("DIA6", "Día 6 h", "#F59E0B"),
            new DefaultEntry("NUEVE_SEIS", "9–6", "#EA580C"),
            new DefaultEntry("NOCHE", "Noche", "#0B192C"),
            new DefaultEntry("APOYO", "Apoyo", "#0284C7"),
            new DefaultEntry("DESCANSO", "Descanso", "#64748B")
    );

    private static final List<ColorPreset> CURATED_PALETTE = List.of(
            new ColorPreset("Naranja Energía", "#FF6B00"),
            new ColorPreset("Naranja Intenso", "#EA580C"),
            new ColorPreset("Ámbar Profundo", "#D97706"),
            new ColorPreset("Ámbar Vivo", "#F59E0B"),
            new ColorPreset("Azul Petróleo", "#0284C7"),
            new ColorPreset("Azul Rey", "#2563EB"),
            new ColorPreset("Azul Midnight", "#0B192C"),
            new ColorPreset("Verde Azulado", "#0F766E"),
            new ColorPreset("Esmeralda", "#10B981"),
            new ColorPreset("Verde Bosque", "#16A34A"),
            new ColorPreset("Índigo", "#6366F1"),
            new ColorPreset("Violeta", "#7C3AED"),
            new ColorPreset("Púrpura", "#9333EA"),
            new ColorPreset("Magenta", "#DB2777"),
            new ColorPreset("Carmesí", "#DC2626"),
            new ColorPreset("Pizarra", "#64748B")
    );

    private static final Map<String, DefaultEntry> FUEL_BY_KEY = indexByKey(FUEL_DEFAULTS);
    private static final Map<String, DefaultEntry> SHIFT_BY_KEY = indexByKey(SHIFT_DEFAULTS);

    private ColorCatalog() {
    }

    public static List<DefaultEntry> fuelDefaults() {
        return FUEL_DEFAULTS;
    }

    public static List<DefaultEntry> shiftDefaults() {
        return SHIFT_DEFAULTS;
    }

    public static List<ColorPreset> curatedPalette() {
        return CURATED_PALETTE;
    }

    /** Validates and canonicalizes an entity key for the given category. */
    public static String normalizeAndValidateKey(ColorCategory category, String rawKey) {
        if (category == null) {
            throw new InvalidColorKeyException(null, String.valueOf(rawKey));
        }
        if (rawKey == null || rawKey.isBlank()) {
            throw new InvalidColorKeyException(category, String.valueOf(rawKey));
        }
        String trimmed = rawKey.strip();
        return switch (category) {
            case FUEL -> requireKnownKey(category, trimmed.toUpperCase(Locale.ROOT), FUEL_BY_KEY);
            case SHIFT -> requireKnownKey(category, trimmed.toUpperCase(Locale.ROOT), SHIFT_BY_KEY);
            case WORKER -> parseWorkerUuid(trimmed);
        };
    }

    /** Returns the system default hex for fuels and shift families, or empty for worker entries. */
    public static Optional<String> defaultHex(ColorCategory category, String canonicalKey) {
        DefaultEntry entry = switch (category) {
            case FUEL -> FUEL_BY_KEY.get(canonicalKey);
            case SHIFT -> SHIFT_BY_KEY.get(canonicalKey);
            case WORKER -> null;
        };
        return Optional.ofNullable(entry).map(DefaultEntry::defaultHex);
    }

    private static String requireKnownKey(
            ColorCategory category, String upperKey, Map<String, DefaultEntry> allowed) {
        if (!allowed.containsKey(upperKey)) {
            throw new InvalidColorKeyException(category, upperKey);
        }
        return upperKey;
    }

    private static String parseWorkerUuid(String raw) {
        try {
            return UUID.fromString(raw).toString();
        } catch (IllegalArgumentException ex) {
            throw new InvalidColorKeyException(ColorCategory.WORKER, raw);
        }
    }

    private static Map<String, DefaultEntry> indexByKey(List<DefaultEntry> entries) {
        Map<String, DefaultEntry> map = new LinkedHashMap<>();
        for (DefaultEntry entry : entries) {
            map.put(entry.key(), entry);
        }
        return Map.copyOf(map);
    }
}
