package com.edu.unicauca.gasstation.backend.sales.api.dto;

import com.edu.unicauca.gasstation.backend.sales.domain.services.JournalCellNoteService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request body to create, update, or delete (when note is null/blank) an Excel-style cell note (SR-08).
 */
public record UpsertJournalCellNoteRequest(
        @NotNull(message = "El número de paso es obligatorio.")
        @Min(value = 1, message = "El número de paso debe estar entre 1 y 6.")
        @Max(value = 6, message = "El número de paso debe estar entre 1 y 6.")
        Integer stepNumber,

        @NotBlank(message = "La columna destino es obligatoria.")
        @Size(max = JournalCellNoteService.MAX_KEY_LENGTH, message = "La clave de columna no puede superar los 64 caracteres.")
        String columnKey,

        @NotBlank(message = "El campo destino es obligatorio.")
        @Size(max = JournalCellNoteService.MAX_KEY_LENGTH, message = "La clave de campo no puede superar los 64 caracteres.")
        String fieldKey,

        @Size(max = JournalCellNoteService.MAX_NOTE_LENGTH, message = "La nota de celda no puede superar los 500 caracteres.")
        String note) {
}
