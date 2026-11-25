package com.edu.unicauca.gasstation.backend.sales.api.dto;

import com.edu.unicauca.gasstation.backend.sales.domain.services.JournalService;
import jakarta.validation.constraints.Size;

/**
 * Request body to create, update, or clear operational observations of an open journal (SR-08).
 */
public record UpdateJournalObservationsRequest(
        @Size(
                max = JournalService.MAX_OBSERVATIONS_LENGTH,
                message = "Las observaciones no pueden superar los 2000 caracteres.")
        String observations) {
}
