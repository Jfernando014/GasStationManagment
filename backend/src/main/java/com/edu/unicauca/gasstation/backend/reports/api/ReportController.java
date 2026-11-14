package com.edu.unicauca.gasstation.backend.reports.api;

import com.edu.unicauca.gasstation.backend.reports.application.BalanceReportService;
import com.edu.unicauca.gasstation.backend.reports.application.DominusComplementReportService;
import com.edu.unicauca.gasstation.backend.reports.application.VariationReportService;
import com.edu.unicauca.gasstation.backend.reports.exception.ReportGenerationException;
import com.edu.unicauca.gasstation.backend.reports.infrastructure.export.BalancePresentationExporter;
import com.edu.unicauca.gasstation.backend.reports.infrastructure.export.DominusComplementWorkbookExporter;
import com.edu.unicauca.gasstation.backend.reports.infrastructure.export.VariationWorkbookExporter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.IOException;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Report downloads of the reports module. Every endpoint is ADMIN-only.
 *
 * @author javiersolanop777
 */
@Tag(name = "Reports", description = "VARIACIÓN, COMPLEMENTO DOMINUS and executive balance downloads")
@RestController
@RequestMapping("/api/v1/reports")
public class ReportController {

    public static final String XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    public static final String PPTX = "application/vnd.openxmlformats-officedocument.presentationml.presentation";

    private final VariationReportService variationReportService;
    private final DominusComplementReportService dominusComplementReportService;
    private final BalanceReportService balanceReportService;
    private final VariationWorkbookExporter variationWorkbookExporter;
    private final DominusComplementWorkbookExporter dominusComplementWorkbookExporter;
    private final BalancePresentationExporter balancePresentationExporter;

    public ReportController(VariationReportService variationReportService,
                            DominusComplementReportService dominusComplementReportService,
                            BalanceReportService balanceReportService,
                            VariationWorkbookExporter variationWorkbookExporter,
                            DominusComplementWorkbookExporter dominusComplementWorkbookExporter,
                            BalancePresentationExporter balancePresentationExporter) {
        this.variationReportService = variationReportService;
        this.dominusComplementReportService = dominusComplementReportService;
        this.balanceReportService = balanceReportService;
        this.variationWorkbookExporter = variationWorkbookExporter;
        this.dominusComplementWorkbookExporter = dominusComplementWorkbookExporter;
        this.balancePresentationExporter = balancePresentationExporter;
    }

    @Operation(summary = "Download the VARIACIÓN Excel of one day (ADMIN)")
    @ApiResponse(responseCode = "200", description = "Excel file (.xlsx)",
            content = @Content(mediaType = XLSX, schema = @Schema(type = "string", format = "binary")))
    @GetMapping(value = "/variation/{date}", produces = XLSX)
    public ResponseEntity<byte[]> downloadVariation(
            @Parameter(description = "Operational date (yyyy-MM-dd)", example = "2026-08-01")
            @PathVariable("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            Authentication authentication) {
        ReportAdminAuthorization.requireAdmin(authentication);
        try {
            byte[] content = variationWorkbookExporter.export(variationReportService.build(date));
            return file(content, "variacion-" + date + ".xlsx", XLSX);
        } catch (IOException exception) {
            throw new ReportGenerationException("No se pudo generar el archivo VARIACIÓN", exception);
        }
    }

    @Operation(summary = "Download the COMPLEMENTO DOMINUS Excel of one day (ADMIN)")
    @ApiResponse(responseCode = "200", description = "Excel file (.xlsx)",
            content = @Content(mediaType = XLSX, schema = @Schema(type = "string", format = "binary")))
    @GetMapping(value = "/dominus-complement/{date}", produces = XLSX)
    public ResponseEntity<byte[]> downloadDominusComplement(
            @Parameter(description = "Operational date (yyyy-MM-dd)", example = "2026-08-01")
            @PathVariable("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            Authentication authentication) {
        ReportAdminAuthorization.requireAdmin(authentication);
        try {
            byte[] content = dominusComplementWorkbookExporter.export(dominusComplementReportService.build(date));
            return file(content, "complemento-dominus-" + date + ".xlsx", XLSX);
        } catch (IOException exception) {
            throw new ReportGenerationException("No se pudo generar el archivo COMPLEMENTO DOMINUS", exception);
        }
    }

    @Operation(summary = "Download the executive balance PowerPoint of a period (ADMIN)")
    @ApiResponse(responseCode = "200", description = "PowerPoint file (.pptx)",
            content = @Content(mediaType = PPTX, schema = @Schema(type = "string", format = "binary")))
    @GetMapping(value = "/balance", produces = PPTX)
    public ResponseEntity<byte[]> downloadBalance(
            @Parameter(description = "First day of the period (yyyy-MM-dd)", example = "2026-08-01")
            @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @Parameter(description = "Last day of the period (yyyy-MM-dd)", example = "2026-08-31")
            @RequestParam("to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            Authentication authentication) {
        ReportAdminAuthorization.requireAdmin(authentication);
        try {
            byte[] content = balancePresentationExporter.export(balanceReportService.build(from, to));
            return file(content, "balance-operativo-" + from + "_" + to + ".pptx", PPTX);
        } catch (IOException exception) {
            throw new ReportGenerationException("No se pudo generar el balance en PowerPoint", exception);
        }
    }

    private static ResponseEntity<byte[]> file(byte[] content, String fileName, String mediaType) {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(mediaType))
                .contentLength(content.length)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(fileName).build().toString())
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(content);
    }
}
