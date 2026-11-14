package com.edu.unicauca.gasstation.backend.backup.api;

import com.edu.unicauca.gasstation.backend.backup.api.dto.BackupAuditResponse;
import com.edu.unicauca.gasstation.backend.backup.api.dto.RestoreResponse;
import com.edu.unicauca.gasstation.backend.backup.application.BackupService;
import com.edu.unicauca.gasstation.backend.backup.exception.BackupException;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

/**
 * Admin endpoints for exporting, importing and listing database backups.
 *
 * @author javiersolanop777
 */
@RestController
@RequestMapping("/api/v1/backups")
@RequiredArgsConstructor
public class BackupController {

    private final BackupService atrBackupService;

    @GetMapping("/export")
    public ResponseEntity<StreamingResponseBody> export(Authentication prmAuthentication,
            HttpServletRequest prmRequest) {
        BackupService.ExportFile objFile = atrBackupService.exportBackup(
                prmAuthentication.getName(), clientIp(prmRequest));

        StreamingResponseBody objBody = prmOutput -> {
            try (InputStream objInput = Files.newInputStream(objFile.path())) {
                objInput.transferTo(prmOutput);
            } finally {
                Files.deleteIfExists(objFile.path());
            }
        };

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(objFile.sizeBytes())
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(objFile.fileName()).build().toString())
                .body(objBody);
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<RestoreResponse> importBackup(
            @RequestParam("file") MultipartFile prmFile,
            @RequestHeader(value = "X-Confirm-Restore", required = false) String prmConfirmation,
            Authentication prmAuthentication,
            HttpServletRequest prmRequest) throws IOException {
        if (prmFile == null || prmFile.isEmpty()) {
            throw new BackupException(BackupException.Reason.INVALID_FILE, "The backup file is empty");
        }

        String objFileName = cleanFileName(prmFile.getOriginalFilename());
        try (InputStream objInput = prmFile.getInputStream()) {
            BackupService.RestoreResult objResult = atrBackupService.importBackup(objInput,
                    prmFile.getSize(), objFileName, prmConfirmation,
                    prmAuthentication.getName(), clientIp(prmRequest));
            RestoreResponse objResponse = new RestoreResponse(objResult.fileName(),
                    objResult.sizeBytes(), objResult.restartScheduled(), objResult.message());
            return ResponseEntity.ok(objResponse);
        }
    }

    @GetMapping("/audit")
    public ResponseEntity<List<BackupAuditResponse>> audit() {
        List<BackupAuditResponse> objAudit = atrBackupService.findRecentAudit().stream()
                .map(BackupAuditResponse::from)
                .toList();
        return ResponseEntity.ok(objAudit);
    }

    private String cleanFileName(String prmFileName) {
        String objName = prmFileName == null ? "backup.dump" : prmFileName.replace('\\', '/');
        int varSlash = objName.lastIndexOf('/');
        if (varSlash >= 0) {
            objName = objName.substring(varSlash + 1);
        }
        objName = objName.replaceAll("[^A-Za-z0-9._-]", "_");
        if (objName.isBlank()) {
            objName = "backup.dump";
        }
        return objName.length() > 255 ? objName.substring(objName.length() - 255) : objName;
    }

    private String clientIp(HttpServletRequest prmRequest) {
        String objForwarded = prmRequest.getHeader("X-Forwarded-For");
        String objIp = objForwarded == null || objForwarded.isBlank()
                ? prmRequest.getRemoteAddr()
                : objForwarded.split(",")[0].trim();
        return objIp.length() > 64 ? objIp.substring(0, 64) : objIp;
    }
}
