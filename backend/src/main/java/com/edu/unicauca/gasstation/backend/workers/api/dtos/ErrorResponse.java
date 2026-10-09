package com.edu.unicauca.gasstation.backend.workers.api.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Error body of the workers module: {@code { "message": "...", "field": "..." }}.
 * Temporary until the team defines a global error format.
 *
 * @param message message shown to the user, in Spanish
 * @param field   request field that failed validation; omitted from the JSON for other errors
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(String message, String field) {

    /** Error not tied to a request field. */
    public static ErrorResponse of(String message) {
        return new ErrorResponse(message, null);
    }
}
