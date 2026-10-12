package com.edu.unicauca.gasstation.backend.sales.domain.services;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.edu.unicauca.gasstation.backend.sales.domain.JournalFile;
import com.edu.unicauca.gasstation.backend.sales.domain.JournalSnapshot;
import com.edu.unicauca.gasstation.backend.sales.domain.JournalStatus;
import com.edu.unicauca.gasstation.backend.sales.domain.repositories.JournalFileRepository;
import com.edu.unicauca.gasstation.backend.sales.domain.repositories.JournalReadPort;
import com.edu.unicauca.gasstation.backend.sales.exception.InvalidJournalFileException;
import com.edu.unicauca.gasstation.backend.sales.exception.JournalNotFoundException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JournalFileServiceTest {

    private final List<JournalFile> storedFiles = new ArrayList<>();
    private JournalFileService service;

    @BeforeEach
    void setUp() {
        JournalFileRepository fileRepository = new JournalFileRepository() {
            @Override
            public JournalFile save(JournalFile file) {
                storedFiles.removeIf(existing -> existing.journalId().equals(file.journalId())
                        && existing.fileName().equals(file.fileName()));
                JournalFile saved = new JournalFile((long) storedFiles.size() + 1, file.journalId(), file.fileName(),
                        file.contentType(), file.content(), LocalDateTime.now());
                storedFiles.add(saved);
                return saved;
            }

            @Override
            public List<JournalFile> findAllByJournalId(Long journalId) {
                return storedFiles.stream().filter(file -> file.journalId().equals(journalId)).toList();
            }

            @Override
            public Optional<JournalFile> findByIdAndJournalId(Long fileId, Long journalId) {
                return storedFiles.stream()
                        .filter(file -> file.id().equals(fileId) && file.journalId().equals(journalId))
                        .findFirst();
            }
        };
        JournalReadPort journalReadPort = id -> id == 1
                ? Optional.of(new JournalSnapshot(1L, LocalDate.of(2026, 10, 12), 7L, JournalStatus.OPEN,
                LocalDateTime.of(2026, 10, 12, 8, 0), List.of()))
                : Optional.empty();
        service = new JournalFileService(fileRepository, journalReadPort);
    }

    @Test
    void uploadedFileCanBeDownloadedWithIdenticalBytes() {
        byte[] original = new byte[] {0, 1, 2, -1, 42};
        JournalFile uploaded = service.upload(1L, "C:\\fakepath\\cierre.xlsx", original);
        original[0] = 99;

        JournalFile downloaded = service.download(1L, uploaded.id());

        assertEquals("cierre.xlsx", downloaded.fileName());
        assertEquals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", downloaded.contentType());
        assertArrayEquals(new byte[] {0, 1, 2, -1, 42}, downloaded.content());
    }

    @Test
    void rejectsUnsupportedTypesAndEmptyFiles() {
        InvalidJournalFileException unsupported = assertThrows(InvalidJournalFileException.class,
                () -> service.upload(1L, "notas.txt", new byte[] {1}));
        InvalidJournalFileException empty = assertThrows(InvalidJournalFileException.class,
                () -> service.upload(1L, "reporte.pdf", new byte[0]));

        assertEquals(InvalidJournalFileException.Reason.UNSUPPORTED_TYPE, unsupported.getReason());
        assertEquals(InvalidJournalFileException.Reason.EMPTY, empty.getReason());
    }

    @Test
    void rejectsFilesLargerThanConfiguredLimit() {
        byte[] content = new byte[(int) JournalFileService.MAX_FILE_SIZE_BYTES + 1];

        InvalidJournalFileException exception = assertThrows(InvalidJournalFileException.class,
                () -> service.upload(1L, "reporte.pdf", content));

        assertEquals(InvalidJournalFileException.Reason.TOO_LARGE, exception.getReason());
    }

    @Test
    void rejectsUnknownJournal() {
        assertThrows(JournalNotFoundException.class, () -> service.upload(99L, "reporte.pdf", new byte[] {1}));
    }
}