package com.edu.unicauca.gasstation.backend.backup.api.dto;

import com.edu.unicauca.gasstation.backend.backup.domain.models.BackupAction;
import com.edu.unicauca.gasstation.backend.backup.domain.models.BackupAudit;
import com.edu.unicauca.gasstation.backend.backup.domain.models.BackupStatus;
import java.time.Instant;

/**
 * Audit row returned to the frontend.
 *
 * @author javiersolanop777
 */
public record BackupAuditResponse(
        Long id,
        String username,
        BackupAction action,
        String fileName,
        long sizeBytes,
        BackupStatus status,
        String message,
        String clientIp,
        Instant startedAt,
        Instant finishedAt) {

    public static BackupAuditResponse from(BackupAudit prmAudit) {
        return new BackupAuditResponse(prmAudit.id(), prmAudit.username(), prmAudit.action(),
                prmAudit.fileName(), prmAudit.sizeBytes(), prmAudit.status(), prmAudit.message(),
                prmAudit.clientIp(), prmAudit.startedAt(), prmAudit.finishedAt());
    }
}
