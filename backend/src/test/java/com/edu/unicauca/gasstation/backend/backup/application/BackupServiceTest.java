package com.edu.unicauca.gasstation.backend.backup.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.edu.unicauca.gasstation.backend.backup.application.ports.BackupExecutor;
import com.edu.unicauca.gasstation.backend.backup.application.ports.BackupRestarter;
import com.edu.unicauca.gasstation.backend.backup.domain.models.BackupAudit;
import com.edu.unicauca.gasstation.backend.backup.domain.models.BackupStatus;
import com.edu.unicauca.gasstation.backend.backup.domain.repositories.BackupAuditRepository;
import com.edu.unicauca.gasstation.backend.backup.exception.BackupException;
import com.edu.unicauca.gasstation.backend.backup.infrastructure.config.BackupProperties;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Tests the backup service without a real database.
 *
 * @author javiersolanop777
 */
@ExtendWith(MockitoExtension.class)
class BackupServiceTest {

    private static final byte[] DUMP = "PGDMP-test-dump".getBytes(StandardCharsets.US_ASCII);

    @TempDir
    Path atrTempDir;

    @Mock
    private BackupExecutor atrExecutor;

    @Mock
    private BackupAuditRepository atrAuditRepository;

    @Mock
    private BackupRestarter atrRestarter;

    @Mock
    private DataSource atrDataSource;

    @Mock
    private Connection atrConnection;

    @Mock
    private PreparedStatement atrStatement;

    @Mock
    private Flyway atrFlyway;

    private BackupProperties atrProperties;
    private BackupService atrService;

    @BeforeEach
    void setUp() throws Exception {
        atrProperties = new BackupProperties();
        atrProperties.setDir(atrTempDir.toString());
        atrProperties.setMaxUploadBytes(1024);
        atrProperties.setConfirmationPhrase("RESTAURAR");
        atrProperties.setRestartAfterRestore(true);
        when(atrAuditRepository.save(any())).thenAnswer(prmInvocation -> prmInvocation.getArgument(0));
        atrService = new BackupService(atrProperties, atrExecutor, atrAuditRepository, atrRestarter,
                new MaintenanceMode(), atrDataSource, atrFlyway);
    }

    @Test
    void exportBackupWritesTheDumpAndMarksTheAuditAsSucceeded() throws Exception {
        doAnswer(prmInvocation -> {
            Files.write(prmInvocation.getArgument(0), DUMP);
            return null;
        }).when(atrExecutor).dump(any());

        BackupService.ExportFile objFile = atrService.exportBackup("admin", "127.0.0.1");

        assertThat(objFile.fileName()).endsWith(".dump");
        assertThat(objFile.sizeBytes()).isEqualTo(DUMP.length);
        assertThat(Files.readAllBytes(objFile.path())).isEqualTo(DUMP);
        ArgumentCaptor<BackupAudit> objAudit = ArgumentCaptor.forClass(BackupAudit.class);
        verify(atrAuditRepository, times(2)).save(objAudit.capture());
        assertThat(objAudit.getValue().status()).isEqualTo(BackupStatus.SUCCEEDED);
        Files.deleteIfExists(objFile.path());
    }

    @Test
    void importBackupRequiresTheConfirmationPhrase() {
        assertThatThrownBy(() -> atrService.importBackup(InputStream.nullInputStream(), 10L,
                "backup.dump", "NO", "admin", "127.0.0.1"))
                .isInstanceOf(BackupException.class)
                .extracting(prmException -> ((BackupException) prmException).getReason())
                .isEqualTo(BackupException.Reason.CONFIRMATION_REQUIRED);
        verify(atrExecutor, never()).restore(any());
    }

    @Test
    void importBackupRejectsAFileLargerThanTheLimit() {
        assertThatThrownBy(() -> atrService.importBackup(InputStream.nullInputStream(), 2_048L,
                "backup.dump", "RESTAURAR", "admin", "127.0.0.1"))
                .isInstanceOf(BackupException.class)
                .extracting(prmException -> ((BackupException) prmException).getReason())
                .isEqualTo(BackupException.Reason.FILE_TOO_LARGE);
        verify(atrExecutor, never()).restore(any());
    }

    @Test
    void importBackupRestoresTheDumpAndSchedulesTheRestart() throws Exception {
        when(atrDataSource.getConnection()).thenReturn(atrConnection);
        when(atrConnection.prepareStatement(any())).thenReturn(atrStatement);
        when(atrDataSource.isWrapperFor(any())).thenReturn(false);
        doAnswer(prmInvocation -> {
            Files.write(prmInvocation.getArgument(0), DUMP);
            return null;
        }).when(atrExecutor).dump(any());

        BackupService.RestoreResult objResult = atrService.importBackup(
                new java.io.ByteArrayInputStream(DUMP), DUMP.length, "backup.dump",
                "RESTAURAR", "admin", "127.0.0.1");

        assertThat(objResult.restartScheduled()).isTrue();
        assertThat(objResult.sizeBytes()).isEqualTo(DUMP.length);
        verify(atrExecutor).restore(any());
        verify(atrFlyway).migrate();
        verify(atrRestarter).restartAfterRestore();
        verify(atrStatement).execute();
        assertThat(Files.list(atrTempDir).count()).isZero();
    }

    @Test
    void importBackupKeepsTheUploadedSizeWhenTheRestoreFails() throws Exception {
        when(atrDataSource.getConnection()).thenReturn(atrConnection);
        when(atrConnection.prepareStatement(any())).thenReturn(atrStatement);
        when(atrDataSource.isWrapperFor(any())).thenReturn(false);
        doAnswer(prmInvocation -> {
            Files.write(prmInvocation.getArgument(0), DUMP);
            return null;
        }).when(atrExecutor).dump(any());
        doThrow(new BackupException(BackupException.Reason.RESTORE_FAILED, "restore failed"))
                .when(atrExecutor).restore(any());

        assertThatThrownBy(() -> atrService.importBackup(new java.io.ByteArrayInputStream(DUMP),
                DUMP.length, "backup.dump", "RESTAURAR", "admin", "127.0.0.1"))
                .isInstanceOf(BackupException.class)
                .extracting(prmException -> ((BackupException) prmException).getReason())
                .isEqualTo(BackupException.Reason.RESTORE_FAILED);

        ArgumentCaptor<BackupAudit> objAudit = ArgumentCaptor.forClass(BackupAudit.class);
        verify(atrAuditRepository, times(3)).save(objAudit.capture());
        assertThat(objAudit.getValue().status()).isEqualTo(BackupStatus.FAILED);
        assertThat(objAudit.getValue().sizeBytes()).isEqualTo(DUMP.length);
        assertThat(Files.list(atrTempDir).count()).isZero();
    }
}
