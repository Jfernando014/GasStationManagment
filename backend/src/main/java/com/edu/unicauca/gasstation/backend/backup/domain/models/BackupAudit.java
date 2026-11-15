package com.edu.unicauca.gasstation.backend.backup.domain.models;

import java.time.Instant;

/**
 * One audit row for a backup or restore operation.
 *
 * @author javiersolanop777
 */
public record BackupAudit(
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

    /** Returns a copy with the final status of the operation. */
    public BackupAudit finish(BackupStatus prmStatus, String prmMessage, Instant prmFinishedAt) {
        return new BackupAudit(id, username, action, fileName, sizeBytes, prmStatus,
                prmMessage, clientIp, startedAt, prmFinishedAt);
    }

    /** Returns a copy with the final file name and size. */
    public BackupAudit withFile(String prmFileName, long prmSizeBytes) {
        return new BackupAudit(id, username, action, prmFileName, prmSizeBytes, status,
                message, clientIp, startedAt, finishedAt);
    }
}
