package com.edu.unicauca.gasstation.backend.appearance.domain.services;

import com.edu.unicauca.gasstation.backend.appearance.domain.models.ColorCatalog;
import com.edu.unicauca.gasstation.backend.appearance.domain.models.ColorCategory;
import com.edu.unicauca.gasstation.backend.appearance.domain.models.ColorHex;
import com.edu.unicauca.gasstation.backend.appearance.domain.models.ColorSetting;
import com.edu.unicauca.gasstation.backend.appearance.domain.models.ColorTheme;
import com.edu.unicauca.gasstation.backend.appearance.domain.models.ColorUpdateCommand;
import com.edu.unicauca.gasstation.backend.appearance.domain.models.ResolvedColorEntry;
import com.edu.unicauca.gasstation.backend.appearance.domain.repositories.ColorSettingRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Use cases for reading, updating, and resetting editable UI color preferences for fuels, shift families, and sellers.
 */
@Service
public class ColorSettingService {

    private final ColorSettingRepository repository;
    private final Clock clock;

    @Autowired
    public ColorSettingService(ColorSettingRepository repository) {
        this(repository, Clock.systemUTC());
    }

    public ColorSettingService(ColorSettingRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    /** Returns the full color theme combining persisted overrides with system defaults. */
    @Transactional(readOnly = true)
    public ColorTheme getTheme() {
        List<ColorSetting> stored = repository.findAll();
        Map<ColorCategory, Map<String, String>> byCategory = new EnumMap<>(ColorCategory.class);
        for (ColorCategory category : ColorCategory.values()) {
            byCategory.put(category, new LinkedHashMap<>());
        }
        for (ColorSetting setting : stored) {
            byCategory.get(setting.category()).put(setting.entityKey(), setting.color().value());
        }

        List<ResolvedColorEntry> fuels = resolveEntries(ColorCatalog.fuelDefaults(), byCategory.get(ColorCategory.FUEL));
        List<ResolvedColorEntry> shifts = resolveEntries(ColorCatalog.shiftDefaults(), byCategory.get(ColorCategory.SHIFT));
        Map<String, String> workers = Map.copyOf(new TreeMap<>(byCategory.get(ColorCategory.WORKER)));

        return new ColorTheme(fuels, shifts, workers, ColorCatalog.curatedPalette());
    }

    /** Updates a single entity color (or removes the override if it matches the system default). */
    @Transactional
    public ColorTheme upsertSingle(ColorCategory category, String entityKey, String rawHex) {
        String canonicalKey = ColorCatalog.normalizeAndValidateKey(category, entityKey);
        ColorHex validatedHex = ColorHex.of(rawHex);
        applyChange(category, canonicalKey, validatedHex, Instant.now(clock));
        return getTheme();
    }

    /**
     * Atomically applies a batch of color updates. Every entry is validated before mutating persistence so invalid
     * batches never persist partial state. A null or blank hex resets that key to its default.
     */
    @Transactional
    public ColorTheme updateBatch(List<ColorUpdateCommand> commands) {
        if (commands == null || commands.isEmpty()) {
            return getTheme();
        }
        record ValidatedChange(ColorCategory category, String key, ColorHex colorOrNull) {
        }
        record CompositeKey(ColorCategory category, String key) {
        }

        Map<CompositeKey, ValidatedChange> deduplicated = new LinkedHashMap<>();
        for (ColorUpdateCommand command : commands) {
            String canonicalKey = ColorCatalog.normalizeAndValidateKey(command.category(), command.entityKey());
            ColorHex parsedHex = (command.colorHex() == null || command.colorHex().isBlank())
                    ? null
                    : ColorHex.of(command.colorHex());
            deduplicated.put(
                    new CompositeKey(command.category(), canonicalKey),
                    new ValidatedChange(command.category(), canonicalKey, parsedHex));
        }

        Instant now = Instant.now(clock);
        for (ValidatedChange change : deduplicated.values()) {
            applyChange(change.category(), change.key(), change.colorOrNull(), now);
        }
        return getTheme();
    }

    /** Resets a single key back to its default color. */
    @Transactional
    public ColorTheme resetSingle(ColorCategory category, String entityKey) {
        String canonicalKey = ColorCatalog.normalizeAndValidateKey(category, entityKey);
        repository.deleteByCategoryAndEntityKey(category, canonicalKey);
        return getTheme();
    }

    /** Resets all overrides, or only overrides belonging to {@code category} when provided. */
    @Transactional
    public ColorTheme resetAll(ColorCategory category) {
        if (category == null) {
            repository.deleteAll();
        } else {
            repository.deleteByCategory(category);
        }
        return getTheme();
    }

    private void applyChange(ColorCategory category, String canonicalKey, ColorHex colorOrNull, Instant now) {
        if (colorOrNull == null) {
            repository.deleteByCategoryAndEntityKey(category, canonicalKey);
            return;
        }
        Optional<String> systemDefault = ColorCatalog.defaultHex(category, canonicalKey);
        if (systemDefault.isPresent() && systemDefault.get().equals(colorOrNull.value())) {
            repository.deleteByCategoryAndEntityKey(category, canonicalKey);
            return;
        }
        ColorSetting setting = repository.findByCategoryAndEntityKey(category, canonicalKey)
                .map(existing -> existing.withColor(colorOrNull, now))
                .orElseGet(() -> ColorSetting.create(category, canonicalKey, colorOrNull, now));
        repository.save(setting);
    }

    private static List<ResolvedColorEntry> resolveEntries(
            List<ColorCatalog.DefaultEntry> defaults, Map<String, String> overrides) {
        return defaults.stream()
                .map(entry -> {
                    String override = overrides.get(entry.key());
                    boolean customized = override != null && !override.equals(entry.defaultHex());
                    String resolvedHex = customized ? override : entry.defaultHex();
                    return new ResolvedColorEntry(entry.key(), entry.label(), resolvedHex, entry.defaultHex(), customized);
                })
                .toList();
    }
}
