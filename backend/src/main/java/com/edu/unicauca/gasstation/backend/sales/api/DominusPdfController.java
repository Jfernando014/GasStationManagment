package com.edu.unicauca.gasstation.backend.sales.api;

import com.edu.unicauca.gasstation.backend.sales.application.DominusPdfService;
import com.edu.unicauca.gasstation.backend.sales.domain.models.DominusCashComplement;
import com.edu.unicauca.gasstation.backend.sales.exception.DominusPdfException;
import com.edu.unicauca.gasstation.backend.sales.exception.DominusPdfException.Reason;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.io.IOException;
import java.util.Locale;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/sales/dominus")
@Tag(name = "Dominus PDF", description = "Append a supplied cash complement without performing cash calculations (SCRUM-38)")
public class DominusPdfController {
    private final DominusPdfService service;

    public DominusPdfController(DominusPdfService service) {
        this.service = service;
    }

    @Operation(summary = "Append one cash complement page to an original Dominus PDF (ADMIN)")
    @PostMapping(value = "/cash-complement", consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> append(
            @RequestPart("file") MultipartFile file,
            @Valid @RequestPart("data") DominusCashComplement data,
            Authentication authentication) {
        SalesAdminAuthorization.requireAdmin(authentication);
        if (file.isEmpty()) {
            throw new DominusPdfException(Reason.EMPTY, "Original PDF must not be empty");
        }
        if (file.getSize() > DominusPdfService.MAX_FILE_SIZE_BYTES) {
            throw new DominusPdfException(Reason.TOO_LARGE, "Original PDF exceeds the 25 MB limit");
        }
        String name = file.getOriginalFilename();
        if (name == null || !name.toLowerCase(Locale.ROOT).endsWith(".pdf")
                || !MediaType.APPLICATION_PDF_VALUE.equalsIgnoreCase(file.getContentType())) {
            throw new DominusPdfException(Reason.UNSUPPORTED_TYPE, "File must have a .pdf extension and application/pdf content type");
        }
        byte[] original;
        try {
            original = file.getBytes();
        } catch (IOException ex) {
            throw new DominusPdfException(Reason.INVALID_PDF, "Original PDF could not be read", ex);
        }
        byte[] result = service.appendCashComplement(original, data);
        String downloadName = "dominus-%s-complemento.pdf".formatted(data.header().reportDate());
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(result.length)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(downloadName).build().toString())
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(result);
    }
}
