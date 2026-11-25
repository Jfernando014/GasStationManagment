package com.edu.unicauca.gasstation.backend.sales.api;

import com.edu.unicauca.gasstation.backend.sales.api.dto.JournalFileResponse;
import com.edu.unicauca.gasstation.backend.sales.application.JournalFullWorkbookService;
import com.edu.unicauca.gasstation.backend.sales.domain.JournalFile;
import com.edu.unicauca.gasstation.backend.sales.domain.JournalFullWorkbookData;
import com.edu.unicauca.gasstation.backend.sales.domain.JournalSnapshot;
import com.edu.unicauca.gasstation.backend.sales.domain.services.JournalFileService;
import com.edu.unicauca.gasstation.backend.sales.exception.JournalFileReadException;
import com.edu.unicauca.gasstation.backend.sales.exception.JournalWorkbookGenerationException;
import com.edu.unicauca.gasstation.backend.sales.infrastructure.export.JournalWorkbookExporter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@Tag(name = "Journal Files", description = "Upload, download and full Excel workbook export for a journal")
@RestController
@RequestMapping("/api/v1/journals/{journalId}")
public class JournalFileController {

    private static final String XLSX_MIME =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    private static final MediaType XLSX_MEDIA_TYPE = MediaType.parseMediaType(XLSX_MIME);

    private final JournalFileService journalFileService;
    private final JournalWorkbookExporter workbookExporter;
    private final JournalFullWorkbookService fullWorkbookService;

    public JournalFileController(JournalFileService journalFileService, JournalWorkbookExporter workbookExporter) {
        this(journalFileService, workbookExporter, null);
    }

    @Autowired
    public JournalFileController(
            JournalFileService journalFileService,
            JournalWorkbookExporter workbookExporter,
            JournalFullWorkbookService fullWorkbookService) {
        this.journalFileService = journalFileService;
        this.workbookExporter = workbookExporter;
        this.fullWorkbookService = fullWorkbookService;
    }

    @PostMapping(value = "/files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<JournalFileResponse> upload(
            @PathVariable Long journalId,
            @RequestParam("file") MultipartFile file) {
        byte[] content;
        try {
            content = file.getBytes();
        } catch (IOException ex) {
            throw new JournalFileReadException(ex);
        }

        JournalFile saved = journalFileService.upload(journalId, file.getOriginalFilename(), content);
        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/v1/journals/{journalId}/files/{fileId}")
                .buildAndExpand(journalId, saved.id())
                .toUri();
        return ResponseEntity.created(location).body(JournalFileResponse.from(saved));
    }

    @GetMapping("/files")
    public List<JournalFileResponse> list(@PathVariable Long journalId) {
        return journalFileService.list(journalId).stream().map(JournalFileResponse::from).toList();
    }

    @GetMapping("/files/{fileId}")
    public ResponseEntity<byte[]> download(
            @PathVariable Long journalId,
            @PathVariable Long fileId) {
        JournalFile file = journalFileService.download(journalId, fileId);
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(file.fileName(), StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.contentType()))
                .contentLength(file.sizeBytes())
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(file.content());
    }

    @Operation(
            summary = "Download the full consolidated Excel workbook (.xlsx) of a journal",
            description = "Exports journal metadata, steps, assigned staff, vendor closes, products, cash balance, and fuel variation/alerts.")
    @ApiResponse(
            responseCode = "200",
            description = "Consolidated journal Excel workbook (.xlsx)",
            content = @Content(mediaType = XLSX_MIME, schema = @Schema(type = "string", format = "binary")))
    @ApiResponse(responseCode = "404", description = "Journal not found")
    @GetMapping("/workbook.xlsx")
    public ResponseEntity<byte[]> downloadWorkbook(@PathVariable Long journalId) {
        JournalFullWorkbookData workbookData = fullWorkbookService != null
                ? fullWorkbookService.buildWorkbookData(journalId)
                : JournalFullWorkbookData.fromSnapshot(journalFileService.getWorkbookData(journalId));
        JournalSnapshot journal = workbookData.journal();
        byte[] workbook;
        try {
            workbook = workbookExporter.export(workbookData);
        } catch (IOException ex) {
            throw new JournalWorkbookGenerationException(ex);
        }
        String fileName = "jornada-%d-%s.xlsx".formatted(journal.consecutive(), journal.date());
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(fileName, StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.status(HttpStatus.OK)
                .contentType(XLSX_MEDIA_TYPE)
                .contentLength(workbook.length)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(workbook);
    }
}