package com.edu.unicauca.gasstation.backend.backup.api.dto;

/**
 * Result of a successful restore.
 *
 * @author javiersolanop777
 */
public record RestoreResponse(
        String fileName,
        long sizeBytes,
        boolean restartScheduled,
        String message) {
}
