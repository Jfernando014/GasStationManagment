package com.edu.unicauca.gasstation.backend.sales.api.dto;

import com.edu.unicauca.gasstation.backend.sales.domain.JournalFile;
import java.time.LocalDateTime;

public record JournalFileResponse(
        Long id,
        String fileName,
        String contentType,
        long sizeBytes,
        LocalDateTime uploadedAt) {

    public static JournalFileResponse from(JournalFile file) {
        return new JournalFileResponse(file.id(), file.fileName(), file.contentType(),
                file.sizeBytes(), file.uploadedAt());
    }
}