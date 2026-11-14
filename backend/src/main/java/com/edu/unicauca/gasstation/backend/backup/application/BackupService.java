package com.edu.unicauca.gasstation.backend.backup.application;

import com.edu.unicauca.gasstation.backend.backup.application.ports.BackupExecutor;
import com.edu.unicauca.gasstation.backend.backup.application.ports.BackupRestarter;
import com.edu.unicauca.gasstation.backend.backup.domain.models.BackupAction;
import com.edu.unicauca.gasstation.backend.backup.domain.models.BackupAudit;
import com.edu.unicauca.gasstation.backend.backup.domain.models.BackupStatus;
import com.edu.unicauca.gasstation.backend.backup.domain.repositories.BackupAuditRepository;
import com.edu.unicauca.gasstation.backend.backup.exception.BackupException;
import com.edu.unicauca.gasstation.backend.backup.infrastructure.config.BackupProperties;
import com.zaxxer.hikari.HikariDataSource;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;
import javax.sql.DataSource;
import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.springframework.stereotype.Service;

/**
 * Generates, restores and records database backups.
 *
 * @author javiersolanop777
 */
@Slf4j
@Service
public class BackupService {

    private static final DateTimeFormatter FILE_DATE = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    private static final byte[] DUMP_HEADER = "PGDMP".getBytes(java.nio.charset.StandardCharsets.US_ASCII);
    private static final int COPY_BUFFER = 8_192;

    private final BackupProperties atrProperties;
    private final BackupExecutor atrExecutor;
    private final BackupAuditRepository atrAuditRepository;
    private final BackupRestarter atrRestarter;
    private final MaintenanceMode atrMaintenanceMode;
    private final DataSource atrDataSource;
    private final Flyway atrFlyway;
    private final ReentrantLock atrLock = new ReentrantLock();

    public BackupService(BackupProperties prmProperties, BackupExecutor prmExecutor,
            BackupAuditRepository prmAuditRepository, BackupRestarter prmRestarter,
            MaintenanceMode prmMaintenanceMode, DataSource prmDataSource, Flyway prmFlyway) {
        this.atrProperties = prmProperties;
        this.atrExecutor = prmExecutor;
        this.atrAuditRepository = prmAuditRepository;
        this.atrRestarter = prmRestarter;
        this.atrMaintenanceMode = prmMaintenanceMode;
        this.atrDataSource = prmDataSource;
        this.atrFlyway = prmFlyway;
    }

    /** A dump ready to be streamed to the client. */
    public record ExportFile(String fileName, Path path, long sizeBytes) {
    }

    /** Result of a successful restore. */
    public record RestoreResult(String fileName, long sizeBytes, boolean restartScheduled, String message) {
    }

    public ExportFile exportBackup(String prmUsername, String prmClientIp) {
        if (!atrLock.tryLock()) {
            throw new BackupException(BackupException.Reason.BUSY, "Another backup operation is running");
        }
        Path objFile = null;
        BackupAudit objAudit = startAudit(BackupAction.EXPORT, prmUsername, null, 0L, prmClientIp);
        try {
            objFile = createTempFile(".dump");
            atrExecutor.dump(objFile);
            validateDump(objFile);
            long varSize = Files.size(objFile);
            String objFileName = "gas-station-" + FILE_DATE.format(LocalDateTime.now()) + ".dump";
            objAudit = finishAudit(objAudit, BackupStatus.SUCCEEDED, "Backup generated", objFileName, varSize);
            return new ExportFile(objFileName, objFile, varSize);
        } catch (BackupException prmException) {
            deleteQuietly(objFile);
            finishAudit(objAudit, BackupStatus.FAILED, prmException.getMessage(), null, 0L);
            throw prmException;
        } catch (IOException prmException) {
            deleteQuietly(objFile);
            finishAudit(objAudit, BackupStatus.FAILED, prmException.getMessage(), null, 0L);
            throw new BackupException(BackupException.Reason.IO_ERROR,
                    "Could not create the backup file", prmException);
        } finally {
            atrLock.unlock();
        }
    }

    public RestoreResult importBackup(InputStream prmInput, long prmContentLength, String prmFileName,
            String prmConfirmation, String prmUsername, String prmClientIp) {
        if (!atrProperties.isRestoreEnabled()) {
            recordFailed(BackupAction.IMPORT, prmUsername, prmFileName, prmClientIp, "Restore is disabled");
            throw new BackupException(BackupException.Reason.RESTORE_DISABLED, "Restore is disabled");
        }
        if (!atrProperties.getConfirmationPhrase().equals(prmConfirmation)) {
            recordFailed(BackupAction.IMPORT, prmUsername, prmFileName, prmClientIp, "Confirmation required");
            throw new BackupException(BackupException.Reason.CONFIRMATION_REQUIRED,
                    "The restore confirmation is not valid");
        }
        if (prmContentLength > atrProperties.getMaxUploadBytes()) {
            recordFailed(BackupAction.IMPORT, prmUsername, prmFileName, prmClientIp, "File is too large");
            throw new BackupException(BackupException.Reason.FILE_TOO_LARGE,
                    "The backup is larger than the allowed limit");
        }
        if (!atrLock.tryLock()) {
            recordFailed(BackupAction.IMPORT, prmUsername, prmFileName, prmClientIp, "Another operation is running");
            throw new BackupException(BackupException.Reason.BUSY, "Another backup operation is running");
        }

        Path objUpload = null;
        Path objSafety = null;
        boolean atrKeepSafety = false;
        long varSize = 0L;
        BackupAudit objAudit = startAudit(BackupAction.IMPORT, prmUsername, prmFileName, 0L, prmClientIp);
        try {
            objUpload = createTempFile(".upload.dump");
            varSize = copyLimited(prmInput, objUpload);
            objAudit = saveAudit(objAudit.withFile(prmFileName, varSize));
            validateDump(objUpload);

            atrMaintenanceMode.activate();
            objSafety = createTempFile(".safety.dump");
            atrExecutor.dump(objSafety);
            validateDump(objSafety);

            terminateOtherSessions();
            evictConnections();
            atrExecutor.restore(objUpload);
            evictConnections();

            try {
                atrFlyway.migrate();
            } catch (RuntimeException prmException) {
                log.error("Flyway failed after the restore. Restoring the safety backup.", prmException);
                atrKeepSafety = !restoreSafety(objSafety);
                throw new BackupException(BackupException.Reason.MIGRATION_FAILED,
                        "The backup was restored but Flyway could not apply the migrations. "
                                + "The previous database was restored.", prmException);
            }

            objAudit = finishAudit(objAudit, BackupStatus.SUCCEEDED, "Restore completed",
                    prmFileName, varSize);
            atrRestarter.restartAfterRestore();
            String objMessage = atrProperties.isRestartAfterRestore()
                    ? "Restore completed. The server will restart."
                    : "Restore completed.";
            return new RestoreResult(prmFileName, varSize, atrProperties.isRestartAfterRestore(), objMessage);
        } catch (BackupException prmException) {
            finishAudit(objAudit, BackupStatus.FAILED, prmException.getMessage(), prmFileName, varSize);
            throw prmException;
        } catch (IOException prmException) {
            finishAudit(objAudit, BackupStatus.FAILED, prmException.getMessage(), prmFileName, varSize);
            throw new BackupException(BackupException.Reason.IO_ERROR,
                    "Could not read the backup file", prmException);
        } finally {
            atrMaintenanceMode.deactivate();
            deleteQuietly(objUpload);
            if (!atrKeepSafety) {
                deleteQuietly(objSafety);
            }
            atrLock.unlock();
        }
    }

    public List<BackupAudit> findRecentAudit() {
        return atrAuditRepository.findRecent(atrProperties.getAuditLimit());
    }

    private long copyLimited(InputStream prmInput, Path prmTarget) throws IOException {
        long varTotal = 0;
        try (OutputStream objOutput = Files.newOutputStream(prmTarget)) {
            byte[] arrBuffer = new byte[COPY_BUFFER];
            int varRead;
            while ((varRead = prmInput.read(arrBuffer)) != -1) {
                varTotal += varRead;
                if (varTotal > atrProperties.getMaxUploadBytes()) {
                    throw new BackupException(BackupException.Reason.FILE_TOO_LARGE,
                            "The backup is larger than the allowed limit");
                }
                objOutput.write(arrBuffer, 0, varRead);
            }
        }
        if (varTotal == 0) {
            throw new BackupException(BackupException.Reason.INVALID_FILE, "The backup file is empty");
        }
        return varTotal;
    }

    private void validateDump(Path prmPath) {
        try (InputStream objInput = Files.newInputStream(prmPath)) {
            byte[] arrHeader = objInput.readNBytes(DUMP_HEADER.length);
            if (!java.util.Arrays.equals(arrHeader, DUMP_HEADER)) {
                throw new BackupException(BackupException.Reason.INVALID_FILE,
                        "The file is not a PostgreSQL custom dump");
            }
        } catch (IOException prmException) {
            throw new BackupException(BackupException.Reason.IO_ERROR,
                    "Could not read the backup file", prmException);
        }
    }

    private void terminateOtherSessions() {
        String objSql = "SELECT pg_terminate_backend(pid) FROM pg_stat_activity "
                + "WHERE datname = current_database() AND pid <> pg_backend_pid()";
        try (Connection objConnection = atrDataSource.getConnection();
                PreparedStatement objStatement = objConnection.prepareStatement(objSql)) {
            objStatement.execute();
        } catch (SQLException prmException) {
            throw new BackupException(BackupException.Reason.RESTORE_FAILED,
                    "Could not close the active database sessions", prmException);
        }
    }

    private void evictConnections() {
        try {
            if (atrDataSource.isWrapperFor(HikariDataSource.class)) {
                HikariDataSource objHikari = atrDataSource.unwrap(HikariDataSource.class);
                if (objHikari.getHikariPoolMXBean() != null) {
                    objHikari.getHikariPoolMXBean().softEvictConnections();
                }
            }
        } catch (SQLException prmException) {
            log.warn("Could not evict the database connections", prmException);
        }
    }

    private boolean restoreSafety(Path prmSafety) {
        if (prmSafety == null) {
            return false;
        }
        try {
            atrExecutor.restore(prmSafety);
            evictConnections();
            return true;
        } catch (RuntimeException prmException) {
            log.error("The safety backup could not be restored: {}", prmSafety.toAbsolutePath(), prmException);
            return false;
        }
    }

    private Path createTempFile(String prmSuffix) {
        try {
            Path objDir = Path.of(atrProperties.getDir());
            Files.createDirectories(objDir);
            return Files.createTempFile(objDir, "gas-station-", prmSuffix);
        } catch (IOException prmException) {
            throw new BackupException(BackupException.Reason.IO_ERROR,
                    "Could not create a temporary backup file", prmException);
        }
    }

    private BackupAudit startAudit(BackupAction prmAction, String prmUsername, String prmFileName,
            long prmSizeBytes, String prmClientIp) {
        BackupAudit objAudit = new BackupAudit(null, prmUsername, prmAction, prmFileName, prmSizeBytes,
                BackupStatus.STARTED, "Started", prmClientIp, Instant.now(), null);
        return saveAudit(objAudit);
    }

    private BackupAudit finishAudit(BackupAudit prmAudit, BackupStatus prmStatus, String prmMessage,
            String prmFileName, long prmSizeBytes) {
        if (prmAudit == null) {
            return null;
        }
        BackupAudit objAudit = prmAudit;
        if (prmFileName != null) {
            objAudit = objAudit.withFile(prmFileName, prmSizeBytes);
        }
        return saveAudit(objAudit.finish(prmStatus, prmMessage, Instant.now()));
    }

    private void recordFailed(BackupAction prmAction, String prmUsername, String prmFileName,
            String prmClientIp, String prmMessage) {
        BackupAudit objAudit = new BackupAudit(null, prmUsername, prmAction, prmFileName, 0L,
                BackupStatus.FAILED, prmMessage, prmClientIp, Instant.now(), Instant.now());
        saveAudit(objAudit);
    }

    private BackupAudit saveAudit(BackupAudit prmAudit) {
        try {
            return atrAuditRepository.save(prmAudit);
        } catch (RuntimeException prmException) {
            log.warn("Could not save the backup audit", prmException);
            return prmAudit;
        }
    }

    private void deleteQuietly(Path prmPath) {
        if (prmPath == null) {
            return;
        }
        try {
            Files.deleteIfExists(prmPath);
        } catch (IOException prmException) {
            log.warn("Could not delete the temporary file {}", prmPath, prmException);
        }
    }
}
