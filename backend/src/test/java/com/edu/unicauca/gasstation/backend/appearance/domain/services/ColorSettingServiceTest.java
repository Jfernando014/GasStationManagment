package com.edu.unicauca.gasstation.backend.appearance.domain.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.edu.unicauca.gasstation.backend.appearance.domain.models.ColorCategory;
import com.edu.unicauca.gasstation.backend.appearance.domain.models.ColorSetting;
import com.edu.unicauca.gasstation.backend.appearance.domain.models.ColorTheme;
import com.edu.unicauca.gasstation.backend.appearance.domain.models.ColorUpdateCommand;
import com.edu.unicauca.gasstation.backend.appearance.domain.repositories.ColorSettingRepository;
import com.edu.unicauca.gasstation.backend.appearance.exception.InsufficientColorContrastException;
import com.edu.unicauca.gasstation.backend.appearance.exception.InvalidColorKeyException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ColorSettingServiceTest {

    private static final UUID WORKER_ID = UUID.fromString("d3c8c22b-9a86-4678-a326-ac90b26057cb");
    private static final Instant NOW = Instant.parse("2026-11-26T17:00:00Z");

    private InMemoryColorSettingRepository repository;
    private ColorSettingService service;

    @BeforeEach
    void setUp() {
        repository = new InMemoryColorSettingRepository();
        service = new ColorSettingService(repository, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void returnsAllDefaultsWhenNoOverridesExist() {
        ColorTheme theme = service.getTheme();

        assertThat(theme.fuels()).hasSize(4);
        assertThat(theme.fuels()).allMatch(entry -> !entry.customized() && entry.colorHex().equals(entry.defaultHex()));
        assertThat(theme.shifts()).hasSize(6);
        assertThat(theme.shifts()).allMatch(entry -> !entry.customized() && entry.colorHex().equals(entry.defaultHex()));
        assertThat(theme.workers()).isEmpty();
        assertThat(theme.palette()).hasSize(16);
    }

    @Test
    void upsertsFuelShiftAndWorkerOverridesAndDeletesWhenRevertedToDefault() {
        ColorTheme updated = service.updateBatch(List.of(
                new ColorUpdateCommand(ColorCategory.FUEL, "motor", "#2563eb"),
                new ColorUpdateCommand(ColorCategory.SHIFT, "noche", "#7c3aed"),
                new ColorUpdateCommand(ColorCategory.WORKER, WORKER_ID.toString().toUpperCase(), "#0f766e")
        ));

        assertThat(updated.fuels().stream().filter(e -> e.key().equals("MOTOR")).findFirst().orElseThrow())
                .satisfies(motor -> {
                    assertThat(motor.colorHex()).isEqualTo("#2563EB");
                    assertThat(motor.customized()).isTrue();
                });
        assertThat(updated.shifts().stream().filter(e -> e.key().equals("NOCHE")).findFirst().orElseThrow())
                .satisfies(night -> {
                    assertThat(night.colorHex()).isEqualTo("#7C3AED");
                    assertThat(night.customized()).isTrue();
                });
        assertThat(updated.workers()).containsEntry(WORKER_ID.toString(), "#0F766E");
        assertThat(repository.findAll()).hasSize(3);

        // Saving the default color removes the override row instead of storing redundant defaults.
        ColorTheme revertedMotor = service.upsertSingle(ColorCategory.FUEL, "MOTOR", "#0284C7");
        assertThat(revertedMotor.fuels().stream().filter(e -> e.key().equals("MOTOR")).findFirst().orElseThrow()
                .customized()).isFalse();
        assertThat(repository.findAll()).hasSize(2);
    }

    @Test
    void validatesEntireBatchBeforePersistingAnyChange() {
        assertThatThrownBy(() -> service.updateBatch(List.of(
                new ColorUpdateCommand(ColorCategory.FUEL, "MOTOR", "#2563EB"),
                new ColorUpdateCommand(ColorCategory.SHIFT, "DIA", "#FFFFFF")
        ))).isInstanceOf(InsufficientColorContrastException.class);

        assertThat(repository.findAll()).isEmpty();
    }

    @Test
    void rejectsUnknownKeysAndInvalidWorkerUuids() {
        assertThatThrownBy(() -> service.upsertSingle(ColorCategory.FUEL, "GLP", "#2563EB"))
                .isInstanceOf(InvalidColorKeyException.class);
        assertThatThrownBy(() -> service.upsertSingle(ColorCategory.SHIFT, "MADRUGADA", "#2563EB"))
                .isInstanceOf(InvalidColorKeyException.class);
        assertThatThrownBy(() -> service.upsertSingle(ColorCategory.WORKER, "not-a-uuid", "#2563EB"))
                .isInstanceOf(InvalidColorKeyException.class);
    }

    @Test
    void resetsSingleCategoryOrAllOverrides() {
        service.updateBatch(List.of(
                new ColorUpdateCommand(ColorCategory.FUEL, "DIESEL", "#0F766E"),
                new ColorUpdateCommand(ColorCategory.SHIFT, "APOYO", "#7C3AED"),
                new ColorUpdateCommand(ColorCategory.WORKER, WORKER_ID.toString(), "#DC2626")
        ));

        ColorTheme afterWorkerReset = service.resetAll(ColorCategory.WORKER);
        assertThat(afterWorkerReset.workers()).isEmpty();
        assertThat(repository.findAll()).hasSize(2);

        ColorTheme afterFullReset = service.resetAll(null);
        assertThat(afterFullReset.fuels()).allMatch(entry -> !entry.customized());
        assertThat(afterFullReset.shifts()).allMatch(entry -> !entry.customized());
        assertThat(repository.findAll()).isEmpty();
    }

    private static final class InMemoryColorSettingRepository implements ColorSettingRepository {

        private final Map<String, ColorSetting> store = new LinkedHashMap<>();

        private static String key(ColorCategory category, String entityKey) {
            return category.name() + "::" + entityKey;
        }

        @Override
        public List<ColorSetting> findAll() {
            return List.copyOf(store.values());
        }

        @Override
        public Optional<ColorSetting> findByCategoryAndEntityKey(ColorCategory category, String entityKey) {
            return Optional.ofNullable(store.get(key(category, entityKey)));
        }

        @Override
        public ColorSetting save(ColorSetting setting) {
            UUID id = setting.id() != null ? setting.id() : UUID.randomUUID();
            ColorSetting saved = new ColorSetting(id, setting.category(), setting.entityKey(), setting.color(),
                    setting.updatedAt());
            store.put(key(setting.category(), setting.entityKey()), saved);
            return saved;
        }

        @Override
        public void deleteByCategoryAndEntityKey(ColorCategory category, String entityKey) {
            store.remove(key(category, entityKey));
        }

        @Override
        public void deleteByCategory(ColorCategory category) {
            store.values().removeIf(setting -> setting.category() == category);
        }

        @Override
        public void deleteAll() {
            store.clear();
        }
    }
}
