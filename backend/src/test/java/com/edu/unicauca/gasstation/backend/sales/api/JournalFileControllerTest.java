package com.edu.unicauca.gasstation.backend.sales.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.edu.unicauca.gasstation.backend.sales.domain.JournalFile;
import com.edu.unicauca.gasstation.backend.sales.domain.JournalSnapshot;
import com.edu.unicauca.gasstation.backend.sales.domain.JournalStatus;
import com.edu.unicauca.gasstation.backend.sales.domain.StepStatus;
import com.edu.unicauca.gasstation.backend.sales.domain.repositories.JournalFileRepository;
import com.edu.unicauca.gasstation.backend.sales.domain.repositories.JournalReadPort;
import com.edu.unicauca.gasstation.backend.sales.domain.services.JournalFileService;
import com.edu.unicauca.gasstation.backend.sales.infrastructure.export.JournalWorkbookExporter;
import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class JournalFileControllerTest {

    private final List<JournalFile> storedFiles = new ArrayList<>();
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        AtomicLong ids = new AtomicLong();
        JournalFileRepository fileRepository = new JournalFileRepository() {
            @Override
            public JournalFile save(JournalFile file) {
                JournalFile existing = storedFiles.stream()
                        .filter(saved -> saved.journalId().equals(file.journalId())
                                && saved.fileName().equals(file.fileName()))
                        .findFirst()
                        .orElse(null);
                if (existing != null) {
                    storedFiles.remove(existing);
                }
                JournalFile saved = new JournalFile(existing == null ? ids.incrementAndGet() : existing.id(),
                        file.journalId(), file.fileName(), file.contentType(), file.content(), LocalDateTime.now());
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
        JournalReadPort journalReadPort = id -> id == 7 ? Optional.of(journalSnapshot()) : Optional.empty();
        JournalFileService service = new JournalFileService(fileRepository, journalReadPort);
        mockMvc = MockMvcBuilders.standaloneSetup(new JournalFileController(service, new JournalWorkbookExporter()))
                .setControllerAdvice(new JournalFileExceptionHandler())
                .build();
    }

    @Test
    void uploadedFileDownloadsWithIdenticalBytes() throws Exception {
        byte[] original = new byte[] {0, 10, -1, 42, 3};
        MockMultipartFile upload = new MockMultipartFile("file", "cierre.csv", "text/csv", original);

        MvcResult uploaded = mockMvc.perform(multipart("/api/v1/journals/7/files").file(upload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.fileName").value("cierre.csv"))
                .andReturn();

        mockMvc.perform(get("/api/v1/journals/7/files/1"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("cierre.csv")))
                .andExpect(content().bytes(original));
        assertNotNull(uploaded.getResponse().getHeader("Location"));
    }

    @Test
    void generatedWorkbookPlacesJourneyValuesInSeparateCells() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/journals/7/workbook.xlsx"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type",
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .andReturn();

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(result.getResponse().getContentAsByteArray()))) {
            assertEquals("Jornada", workbook.getSheetAt(0).getSheetName());
            assertEquals("Campo", workbook.getSheetAt(0).getRow(0).getCell(0).getStringCellValue());
            assertEquals("Fecha", workbook.getSheetAt(0).getRow(2).getCell(0).getStringCellValue());
            assertEquals("2026-10-12", workbook.getSheetAt(0).getRow(2).getCell(1).getStringCellValue());
            assertEquals("Pasos", workbook.getSheetAt(1).getSheetName());
            assertEquals("Nombre del paso", workbook.getSheetAt(1).getRow(0).getCell(1).getStringCellValue());
            assertEquals("Medición inicial", workbook.getSheetAt(1).getRow(1).getCell(1).getStringCellValue());
            assertEquals("COMPLETED", workbook.getSheetAt(1).getRow(1).getCell(2).getStringCellValue());
        }
    }

    @Test
    void unsupportedFileTypeReturnsProblemDetails() throws Exception {
        MockMultipartFile upload = new MockMultipartFile("file", "notas.txt", "text/plain", new byte[] {1});

        mockMvc.perform(multipart("/api/v1/journals/7/files").file(upload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("UNSUPPORTED_JOURNAL_FILE_TYPE"))
                .andExpect(jsonPath("$.title").value("Bad Request"));
    }

    private static JournalSnapshot journalSnapshot() {
        return new JournalSnapshot(7L, LocalDate.of(2026, 10, 12), 42L, JournalStatus.OPEN,
                LocalDateTime.of(2026, 10, 12, 8, 0), List.of(
                new JournalSnapshot.JournalStepSnapshot(1, "Medición inicial", StepStatus.COMPLETED,
                        LocalDateTime.of(2026, 10, 12, 8, 15)),
                new JournalSnapshot.JournalStepSnapshot(2, "Asignación de turnos", StepStatus.PENDING, null)));
    }
}