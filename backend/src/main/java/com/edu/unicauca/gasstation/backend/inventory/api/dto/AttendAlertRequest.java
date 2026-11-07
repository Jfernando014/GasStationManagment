package com.edu.unicauca.gasstation.backend.inventory.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "What the administrator did about the alert")
public class AttendAlertRequest {

    public static final String NOTE_SIZE = "The note cannot exceed 200 characters";

    @Size(max = 200, message = NOTE_SIZE)
    @Schema(description = "Optional note", example = "Fuel ordered, arrives tomorrow")
    private String note;
}