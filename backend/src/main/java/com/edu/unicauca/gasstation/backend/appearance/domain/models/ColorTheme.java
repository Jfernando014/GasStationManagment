package com.edu.unicauca.gasstation.backend.appearance.domain.models;

import java.util.List;
import java.util.Map;

/** Complete color customization snapshot returned by the appearance API. */
public record ColorTheme(
        List<ResolvedColorEntry> fuels,
        List<ResolvedColorEntry> shifts,
        Map<String, String> workers,
        List<ColorPreset> palette) {
}
