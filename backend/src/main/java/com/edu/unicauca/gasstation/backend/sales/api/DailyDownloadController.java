package com.edu.unicauca.gasstation.backend.sales.api;

import com.edu.unicauca.gasstation.backend.sales.application.DailyDownloadService;
import com.edu.unicauca.gasstation.backend.sales.application.DailyDownloadService.DownloadFile;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
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
import org.springframework.web.bind.annotation.RestController;

/**
 * Daily downloads of the end-of-day step (S2-03). Each endpoint returns one Excel file of one journal.
 *
 * @author javiersolanop777
 */
@RestController
@RequestMapping("/api/v1/sales/downloads")
@Tag(name = "Daily Downloads", description = "Excel files of one journal for the end-of-day step (S2-03)")
public class DailyDownloadController {

    public static final String XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private final DailyDownloadService atrService;

    public DailyDownloadController(DailyDownloadService prmService) {
        this.atrService = prmService;
    }

    @Operation(summary = "Download the PLANILLA VENTAS Excel of one journal (ADMIN)",
            description = "One row per vendor (gallons by fuel, additive units, cash, GA, Credibanco, QR, "
                    + "redemptions, delivered, Dominus slip and discrepancy) and one total row for the day. "
                    + "Data comes from SellerCloseQuery.")
    @ApiResponse(responseCode = "200", description = "Excel file (.xlsx)",
            content = @Content(mediaType = XLSX, schema = @Schema(type = "string", format = "binary")))
    @ApiResponse(responseCode = "400", description = "The date is not valid (use yyyy-MM-dd)")
    @ApiResponse(responseCode = "401", description = "Authentication is required")
    @ApiResponse(responseCode = "403", description = "The ADMIN authority is required")
    @ApiResponse(responseCode = "404", description = "There is no closing data for that date")
    @ApiResponse(responseCode = "500", description = "The file could not be built")
    @GetMapping(value = "/{date}/sales-sheet", produces = XLSX)
    public ResponseEntity<byte[]> downloadSalesSheet(
            @Parameter(description = "Journal date (yyyy-MM-dd)", example = "2026-10-24")
            @PathVariable("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate prmDate,
            Authentication prmAuthentication) {
        SalesAdminAuthorization.requireAdmin(prmAuthentication);
        return toResponse(atrService.buildSalesSheet(prmDate));
    }

    @Operation(summary = "Download the LUBRICANTES Y VARIOS Excel of one journal (ADMIN)",
            description = "Products sold in the day with code, group, unit price, units and sale value, "
                    + "totals, and the basket summary: VENTA ACEITES, VENTA MERCANCIA VARIA, "
                    + "VENTA ADITIVOS Y OTROS and VENTA TOTAL CANASTA. Data comes from SellerCloseQuery.")
    @ApiResponse(responseCode = "200", description = "Excel file (.xlsx)",
            content = @Content(mediaType = XLSX, schema = @Schema(type = "string", format = "binary")))
    @ApiResponse(responseCode = "400", description = "The date is not valid (use yyyy-MM-dd)")
    @ApiResponse(responseCode = "401", description = "Authentication is required")
    @ApiResponse(responseCode = "403", description = "The ADMIN authority is required")
    @ApiResponse(responseCode = "404", description = "There is no closing data for that date")
    @ApiResponse(responseCode = "500", description = "The file could not be built")
    @GetMapping(value = "/{date}/lubricants-and-misc", produces = XLSX)
    public ResponseEntity<byte[]> downloadLubricantsAndMisc(
            @Parameter(description = "Journal date (yyyy-MM-dd)", example = "2026-10-24")
            @PathVariable("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate prmDate,
            Authentication prmAuthentication) {
        SalesAdminAuthorization.requireAdmin(prmAuthentication);
        return toResponse(atrService.buildLubricantsAndMisc(prmDate));
    }

    private static ResponseEntity<byte[]> toResponse(DownloadFile prmFile) {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(XLSX))
                .contentLength(prmFile.atrContent().length)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(prmFile.atrFileName()).build().toString())
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(prmFile.atrContent());
    }
}
