package com.edu.unicauca.gasstation.backend.sales.domain.services;

import com.edu.unicauca.gasstation.backend.sales.domain.JournalFile;
import com.edu.unicauca.gasstation.backend.sales.domain.JournalSnapshot;
import com.edu.unicauca.gasstation.backend.sales.domain.repositories.JournalFileRepository;
import com.edu.unicauca.gasstation.backend.sales.domain.repositories.JournalReadPort;
import com.edu.unicauca.gasstation.backend.sales.exception.InvalidJournalFileException;
import com.edu.unicauca.gasstation.backend.sales.exception.JournalFileNotFoundException;
import com.edu.unicauca.gasstation.backend.sales.exception.JournalNotFoundException;
import java.util.List;
import java.util.Locale;

public class JournalFileService {

    public static final long MAX_FILE_SIZE_BYTES = 25L * 1024 * 1024;

    private final JournalFileRepository journalFileRepository;
    private final JournalReadPort journalReadPort;

    public JournalFileService(JournalFileRepository journalFileRepository, JournalReadPort journalReadPort) {
        this.journalFileRepository = journalFileRepository;
        this.journalReadPort = journalReadPort;
    }

    public JournalFile upload(Long journalId, String originalFileName, byte[] content) {
        requireJournal(journalId);
        String fileName = normalizeFileName(originalFileName);
        if (content == null || content.length == 0) {
            throw new InvalidJournalFileException(InvalidJournalFileException.Reason.EMPTY,
                    "El archivo no puede estar vacío");
        }
        if (content.length > MAX_FILE_SIZE_BYTES) {
            throw new InvalidJournalFileException(InvalidJournalFileException.Reason.TOO_LARGE,
                    "El archivo supera el límite de 25 MB");
        }

        String contentType = contentTypeFor(fileName);
        return journalFileRepository.save(new JournalFile(null, journalId, fileName, contentType, content, null));
    }

    public List<JournalFile> list(Long journalId) {
        requireJournal(journalId);
        return journalFileRepository.findAllByJournalId(journalId);
    }

    public JournalFile download(Long journalId, Long fileId) {
        requireJournal(journalId);
        return journalFileRepository.findByIdAndJournalId(fileId, journalId)
                .orElseThrow(() -> new JournalFileNotFoundException(fileId));
    }

    public JournalSnapshot getWorkbookData(Long journalId) {
        return requireJournal(journalId);
    }

    private JournalSnapshot requireJournal(Long journalId) {
        return journalReadPort.findSnapshotById(journalId)
                .orElseThrow(() -> new JournalNotFoundException(journalId));
    }

    private static String normalizeFileName(String originalFileName) {
        if (originalFileName == null || originalFileName.isBlank()) {
            throw new InvalidJournalFileException(InvalidJournalFileException.Reason.INVALID_NAME,
                    "El nombre del archivo es obligatorio");
        }
        String normalized = originalFileName.strip().replace('\\', '/');
        String fileName = normalized.substring(normalized.lastIndexOf('/') + 1);
        if (fileName.isBlank() || fileName.length() > 255 || fileName.chars().anyMatch(Character::isISOControl)) {
            throw new InvalidJournalFileException(InvalidJournalFileException.Reason.INVALID_NAME,
                    "El nombre del archivo no es válido");
        }
        return fileName;
    }

    private static String contentTypeFor(String fileName) {
        int dot = fileName.lastIndexOf('.');
        String extension = dot < 0 ? "" : fileName.substring(dot + 1).toLowerCase(Locale.ROOT);
        return switch (extension) {
            case "csv" -> "text/csv";
            case "xls" -> "application/vnd.ms-excel";
            case "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case "pdf" -> "application/pdf";
            default -> throw new InvalidJournalFileException(InvalidJournalFileException.Reason.UNSUPPORTED_TYPE,
                    "Solo se permiten archivos CSV, Excel (.xls, .xlsx) y PDF");
        };
    }
}