package com.edu.unicauca.gasstation.backend.inventory.api.dto;

import com.edu.unicauca.gasstation.backend.inventory.domain.models.AlertLevel;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.GallonRange;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ShortageRangeResponse {

    @Schema(description = "[1, 49], (49, 74], (74, 124], (124, 249], (249, 499] or more than 499 gallons")
    private GallonRange range;

    private int count;
    private int limit;

    @Schema(description = "OK below the limit, REVISAR on it, ALARMA above it. Null without calculated days")
    private AlertLevel status;
}
