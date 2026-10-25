package com.edu.unicauca.gasstation.backend.sales.api;

import com.edu.unicauca.gasstation.backend.sales.api.dto.PointsTotalRequest;
import com.edu.unicauca.gasstation.backend.sales.application.QrPointsReconciliationService;
import com.edu.unicauca.gasstation.backend.sales.domain.reconciliation.PointsResult;
import com.edu.unicauca.gasstation.backend.sales.domain.reconciliation.QrReconciliationResult;
import com.edu.unicauca.gasstation.backend.sales.domain.reconciliation.RedebanImportResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.io.IOException;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@Tag(name = "QR and Puntos reconciliation", description = "HU-35: Redeban QR by shift and Puntos Colombia redemptions")
@RequestMapping("/api/v1/sales/reconciliation")
@RequiredArgsConstructor
public class QrPointsReconciliationController {
    private final QrPointsReconciliationService service;

    @PostMapping(value = "/qr/{date}/redeban", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Import the Redeban CSV of a day",
            description = "ADMIN only. Sums the approved QR rows of the operative day (06:00 to 06:00) and attributes each to the vendor whose shift covers its hour. "
                    + "Rows without time, outside every shift or covered by two vendors are not guessed: they are reported. "
                    + "Importing again replaces the previous rows of the date.")
    public RedebanImportResult importRedeban(@PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                                @RequestParam("file") MultipartFile file,
                                                Authentication authentication) throws IOException {
        SalesAdminAuthorization.requireAdmin(authentication);
        if (file.isEmpty()) {
            throw new IllegalArgumentException("El archivo CSV está vacío");
        }
        return service.importRedeban(date, file.getBytes());
    }

    @GetMapping("/qr/{date}")
    @Operation(summary = "QR of the day: Redeban against the planillas")
    public QrReconciliationResult qr(@PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return service.qr(date);
    }

    @GetMapping("/qr/{date}/summary/{workerId}")
    @Operation(summary = "WhatsApp text for a vendor",
            description = "Date, vendor, shift, QR total and reconciliation state, ready to copy. Requires the Redeban CSV to be loaded.")
    public java.util.Map<String, String> summary(@PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                                 @PathVariable java.util.UUID workerId) {
        return java.util.Map.of("text", service.whatsappSummary(date, workerId));
    }

    @GetMapping("/points/{date}")
    @Operation(summary = "Puntos Colombia: day total against the planilla redemptions",
            description = "COMPLETED_ZERO when every planilla has 0 redemptions (nothing to type).")
    public PointsResult points(@PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return service.points(date);
    }

    @PutMapping("/points/{date}")
    @Operation(summary = "Load the day total of Puntos Colombia", description = "ADMIN only. Typed until the Excel reading is defined.")
    public PointsResult savePoints(@PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                   @Valid @RequestBody PointsTotalRequest request, Authentication authentication) {
        SalesAdminAuthorization.requireAdmin(authentication);
        return service.savePoints(date, request.total());
    }
}
