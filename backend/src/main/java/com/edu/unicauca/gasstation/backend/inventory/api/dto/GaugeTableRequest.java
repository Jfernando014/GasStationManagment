package com.edu.unicauca.gasstation.backend.inventory.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class GaugeTableRequest {

    public static final String NAME_REQUIRED = "The name is required";
    public static final String NAME_SIZE = "The name cannot be greater than 80 characters";
    public static final String POINTS_REQUIRED = "Must sent the points in the gauge table";

    @Schema(example = "Tanks 1 and 2 (230 cm)")
    @NotBlank(message = NAME_REQUIRED)
    @Size(max = 80, message = NAME_SIZE)
    private String name;

    @Schema(description = "Points in order: strictly increasing height and increasing or constant volume")
    @NotNull(message = POINTS_REQUIRED)
    @Valid
    private List<GaugePointRequest> points;
}