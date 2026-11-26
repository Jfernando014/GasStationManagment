package com.edu.unicauca.gasstation.backend.appearance.api.dtos;

import com.edu.unicauca.gasstation.backend.appearance.domain.models.ColorCategory;
import com.edu.unicauca.gasstation.backend.appearance.domain.models.ColorPreset;
import com.edu.unicauca.gasstation.backend.appearance.domain.models.ColorTheme;
import com.edu.unicauca.gasstation.backend.appearance.domain.models.ColorUpdateCommand;
import com.edu.unicauca.gasstation.backend.appearance.domain.models.ResolvedColorEntry;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;

/** REST DTOs for the appearance color customization API. */
public final class ColorSettingDtos {

    private ColorSettingDtos() {
    }

    @Schema(description = "Resolved color entry for a fuel or shift family")
    public record ResolvedColorEntryResponse(
            @Schema(example = "MOTOR") String key,
            @Schema(example = "Corriente") String label,
            @Schema(example = "#0284C7") String colorHex,
            @Schema(example = "#0284C7") String defaultHex,
            @Schema(example = "false") boolean customized) {

        public static ResolvedColorEntryResponse from(ResolvedColorEntry entry) {
            return new ResolvedColorEntryResponse(
                    entry.key(), entry.label(), entry.colorHex(), entry.defaultHex(), entry.customized());
        }
    }

    @Schema(description = "Curated accessible swatch")
    public record ColorPresetResponse(
            @Schema(example = "Naranja Energía") String name,
            @Schema(example = "#FF6B00") String hex) {

        public static ColorPresetResponse from(ColorPreset preset) {
            return new ColorPresetResponse(preset.name(), preset.hex());
        }
    }

    @Schema(description = "Complete resolved color customization snapshot")
    public record ColorThemeResponse(
            List<ResolvedColorEntryResponse> fuels,
            List<ResolvedColorEntryResponse> shifts,
            Map<String, String> workers,
            List<ColorPresetResponse> palette) {

        public static ColorThemeResponse from(ColorTheme theme) {
            return new ColorThemeResponse(
                    theme.fuels().stream().map(ResolvedColorEntryResponse::from).toList(),
                    theme.shifts().stream().map(ResolvedColorEntryResponse::from).toList(),
                    theme.workers(),
                    theme.palette().stream().map(ColorPresetResponse::from).toList());
        }
    }

    @Schema(description = "Payload to update a single entity color")
    public record UpdateSingleColorRequest(
            @Schema(description = "Hexadecimal color (#RRGGBB) with legible contrast", example = "#0284C7")
            @NotBlank(message = "El color hexadecimal es obligatorio")
            String colorHex) {
    }

    @Schema(description = "Single item inside a batch color update request")
    public record ColorUpdateItemRequest(
            @Schema(example = "FUEL")
            @NotNull(message = "La categoría es obligatoria")
            ColorCategory category,

            @Schema(example = "MOTOR")
            @NotBlank(message = "La clave de la entidad es obligatoria")
            String entityKey,

            @Schema(description = "Hexadecimal color (#RRGGBB), or null/blank to restore default",
                    example = "#2563EB", nullable = true)
            String colorHex) {

        public ColorUpdateCommand toCommand() {
            return new ColorUpdateCommand(category, entityKey, colorHex);
        }
    }

    @Schema(description = "Batch update payload for multiple fuels, shifts, and sellers")
    public record UpdateColorThemeRequest(
            @NotEmpty(message = "Debe incluir al menos un color para actualizar")
            List<@Valid ColorUpdateItemRequest> items) {
    }
}
