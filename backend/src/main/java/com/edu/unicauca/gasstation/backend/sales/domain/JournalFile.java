package com.edu.unicauca.gasstation.backend.sales.domain;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Objects;

public record JournalFile(
        Long id,
        Long journalId,
        String fileName,
        String contentType,
        byte[] content,
        LocalDateTime uploadedAt) {

    public JournalFile {
        content = content.clone();
    }

    @Override
    public byte[] content() {
        return content.clone();
    }

    public long sizeBytes() {
        return content.length;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof JournalFile file)) {
            return false;
        }
        return Objects.equals(id, file.id)
                && Objects.equals(journalId, file.journalId)
                && Objects.equals(fileName, file.fileName)
                && Objects.equals(contentType, file.contentType)
                && Arrays.equals(content, file.content)
                && Objects.equals(uploadedAt, file.uploadedAt);
    }

    @Override
    public int hashCode() {
        return 31 * Objects.hash(id, journalId, fileName, contentType, uploadedAt) + Arrays.hashCode(content);
    }

    @Override
    public String toString() {
        return "JournalFile[id=%s, journalId=%s, fileName=%s, contentType=%s, sizeBytes=%d, uploadedAt=%s]"
                .formatted(id, journalId, fileName, contentType, sizeBytes(), uploadedAt);
    }
}